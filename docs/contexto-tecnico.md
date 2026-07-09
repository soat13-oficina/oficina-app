# Contexto Técnico — Oficina API

Referência técnica descritiva do projeto (módulos, endpoints, padrões de código, testes e
banco). As **regras normativas** vivem na constituição
([`.specify/memory/constitution.md`](../.specify/memory/constitution.md)) — em caso de conflito,
a constituição prevalece. Este documento é o catálogo de "o que existe e como funciona".

Backend monolítico para gestão de oficina mecânica. Java 21 + Spring Boot 4.0.5 + PostgreSQL.

---

## Comandos essenciais

```bash
# Subir banco (pré-requisito)
docker-compose up -d db

# Rodar a aplicação
./mvnw spring-boot:run

# Rodar testes (usa H2 in-memory, sem banco externo)
./mvnw test

# Build completo com verificação de cobertura
./mvnw verify
```

Variáveis de ambiente (todas têm default no `application.yml`):
- `DB_URL` → `jdbc:postgresql://localhost:5432/oficina_db`
- `DB_USER` → `oficina_user`
- `DB_PASS` → `oficina_123`
- `JWT_SECRET` → chave longa hardcoded como fallback
- `ORCAMENTO_WEBHOOK_SECRET` → `segredo-desenvolvimento` (default dev — altere em produção)
- `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` / `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` → configuração do SMTP para notificações

Swagger disponível em `http://localhost:8080/swagger-ui/index.html` quando rodando localmente.

---

## Arquitetura

Arquitetura hexagonal (ports & adapters) com inspiração em CQRS. Cada bounded context segue a mesma estrutura de pacotes:

```
br.com.oficina/<modulo>/
├── domain/
│   ├── model/          # Entidades de domínio ricas (lógica de negócio aqui)
│   └── repository/     # Interfaces (ports) — nunca dependem de JPA
├── application/
│   ├── command/        # Objetos de comando (escrita)
│   ├── query/          # Objetos de query (leitura)
│   ├── usecase/        # Interfaces de caso de uso (1 interface por operação)
│   └── service/        # Implementações dos casos de uso
└── infrastructure/
    ├── persistence/    # Adaptadores JPA (implementam as interfaces do domínio)
    └── web/            # Controllers REST + DTOs de request/response
```

Além dos módulos, existem dois pacotes transversais:

```
br.com.oficina/
├── common/
│   ├── domain/exception/   # RegraDeNegocioException, RecursoNaoEncontradoException
│   └── infrastructure/web/ # ApiExceptionHandler (@RestControllerAdvice)
└── config/
    ├── FlywayConfig.java    # Garantia de ordem: Flyway roda antes do JPA
    └── SwaggerConfig.java   # SpringDoc OpenAPI 2.8.5
```

**Regra crítica:** a camada `domain` nunca importa nada de `infrastructure`. Os repositórios JPA implementam as interfaces do domínio — o domínio não conhece Spring nem JPA.

**Aderência por módulo:**

| Módulo | Hexagonal | Observação |
|---|---|---|
| `cliente` | Parcial | Domain model tem anotações JPA (`@Entity`, `@Column`) |
| `veiculo` | Parcial | Domain model tem anotações JPA (`@Entity`, `@Column`, `@Enumerated`) |
| `ordemservico` | Parcial | `OrdemDeServico` e `Funcionario` têm anotações JPA no aggregate root |
| `orcamento` | Parcial | `Orcamento` tem anotações JPA e `@ElementCollection` no domain |
| `pecainsumo` | **Correto** | `PecaInsumo` é Java puro; `PecaInsumoJpaEntity` é separado com `fromDomain()`/`toDomain()` |
| `notificacao` | **Correto** | Sem repositório próprio; porta `NotificadorEmail` + adaptador Spring Mail; zero JPA |
| `auth` | Simplificado | Sem camada application — acesso direto ao Spring Data e Spring Security |

---

## Módulos

### `auth`
Autenticação JWT stateless. Não segue a estrutura hexagonal — sem camada `application`, acesso direto ao Spring Data.

- `Usuario` (id `Long BIGSERIAL`, email, senha BCrypt) armazenado via `UsuarioRepository` (Spring Data JPA direto)
- JWT gerado por `JwtUtil` (library: `jjwt 0.12.6`), expira em 24h (`jwt.expiration: 86400000`)
- `JwtAuthenticationFilter` valida token em cada requisição (adicionado antes do `UsernamePasswordAuthenticationFilter`)
- `SecurityConfig` — CSRF e CORS desabilitados; sessão stateless; endpoints públicos: `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`
- `POST /api/auth/register` e `POST /api/auth/login` são as únicas rotas públicas — retornam `LoginResponse(token)`
- Todas as outras rotas exigem `Authorization: Bearer <token>`, **exceto** `/integracoes/orcamentos/*/decisao` que é protegida pelo `WebhookTokenFilter` (header `X-Webhook-Token`)
- **Tokens de integração** (`tokens_integracao`, V18): `POST/GET/DELETE /integracoes/tokens` (JWT) geram/listam/revogam tokens opacos para o webhook. O `WebhookTokenFilter` valida o `X-Webhook-Token` contra um token gerado ATIVO (hash SHA-256) **ou** o `ORCAMENTO_WEBHOOK_SECRET` (coexistência temporária; estático descontinuado). Segredo exibido uma vez; armazenado só como hash
- `WebhookTokenFilter` — adicionado antes do `JwtAuthenticationFilter`; rejeita com 401 se o token estiver ausente ou inválido; define `UsernamePasswordAuthenticationToken` para satisfazer o Spring Security

### `cliente`
CRUD completo. Entidade com validação de CPF (11 dígitos) ou CNPJ (14 dígitos), consistente com `TipoCliente` (PF/PJ).
- Repositório: `ClienteRepository` (interface) → `JpaClienteRepository` (adaptador) → `SpringDataClienteRepository`
- Não permite documento duplicado no cadastro
- Campo `email` opcional (string livre, sem validação de formato no domínio) — usado pelo módulo `notificacao` para notificações de status de OS
- **Endpoints:** `POST /clientes` (201), `GET /clientes` (filtro por CPF/CNPJ ou nome), `GET /clientes/{clienteId}` (200), `PUT /clientes/{clienteId}` (204), `DELETE /clientes/{clienteId}` (204)

### `veiculo`
CRUD completo. Pertence a um cliente (validado no cadastro). Placa normalizada e validada em dois formatos:
- Antigo: `ABC1234` (regex `^[A-Z]{3}\d{4}$`)
- Mercosul: `ABC1D23` (regex `^[A-Z]{3}\d[A-Z]\d{2}$`)
- `TipoCombustivel`: `GASOLINA`, `FLEX`, `ELETRICO`, `DIESEL`
- Normalização: `Veiculo.normalizarPlaca()` — UPPERCASE, remove espaços e hifens
- **Endpoints:** `POST /veiculos` (201), `GET /veiculos` (7 filtros: placa, ano, marca, fabricante, potencia, cambio, tipo), `PUT /veiculos/{placa}` (204), `DELETE /veiculos/{placa}` (204)

### `ordemservico`
Bounded context principal. Orquestra o fluxo de trabalho da oficina.

**Situações de negócio (`SituacaoOrdemDeServico`) e mapeamento para estados internos:**

| Situação (exibição) | Estado(s) interno(s) `StatusOrdemDeServico` |
|---|---|
| Recebida | `OS_ABERTA` |
| Diagnóstico | `DIAGNOSTICO_EM_ANDAMENTO`, `DIAGNOSTICO_CONCLUIDO` |
| Aguardando Aprovação | `AGUARDANDO_APROVACAO` |
| Execução | `SERVICO_EM_ANDAMENTO` |
| Finalizada | `OS_FINALIZADA` |
| Entregue | `ENTREGUE` |

**Ciclo de vida — transições implementadas:**

```
OS_ABERTA
  → DIAGNOSTICO_EM_ANDAMENTO  (IniciarDiagnosticoUseCase / iniciarDiagnostico())
  → DIAGNOSTICO_CONCLUIDO     (ConcluirDiagnosticoUseCase / concluirDiagnostico())
  → AGUARDANDO_APROVACAO      (EnviarDiagnosticoParaOrcamentoUseCase / enviarParaAprovacao()
                               — fluxo único: cria o Orçamento e transiciona na mesma transação)
  → SERVICO_EM_ANDAMENTO      (IniciarExecucaoUseCase / iniciarExecucao()
                               — acionado via webhook de aprovação do orçamento)
  → OS_FINALIZADA             (ConcluirServicoUseCase / concluirServico()
                               — motivo: SERVICO_CONCLUIDO)
                              (FinalizarOrdemDeServicoUseCase / finalizar()
                               — legado: direto de DIAGNOSTICO_CONCLUIDO/AGUARDANDO_APROVACAO)
  → ENTREGUE                  (EntregarAoClienteUseCase / entregarAoCliente())

AGUARDANDO_APROVACAO
  → OS_FINALIZADA             (via recusa no webhook / recusarOrcamento()
                               — motivo: ORCAMENTO_RECUSADO)
```

**Campo `motivoEncerramento`:** `MotivoEncerramento.SERVICO_CONCLUIDO` ao concluir o serviço; `MotivoEncerramento.ORCAMENTO_RECUSADO` ao recusar o orçamento. Exposto na consulta de status.

**Value objects do domínio (coleções `@ElementCollection`):**
- `ServicoOrdem(descricao, valorMaoDeObra)` — serviços realizados na OS
- `PecaPrevistaOrdem(pecaInsumoId, quantidade)` — peças previstas; `pecaInsumoId` é `String`

**Entidades do domínio:**
- `OrdemDeServico` — aggregate root. Armazena dados desnormalizados de Cliente, Veículo e Funcionário (snap no momento da abertura). Tem `@Entity` diretamente no aggregate root (antipadrão — ver seção "O que NÃO fazer").
- `Funcionario` — entidade local ao módulo, representa o mecânico responsável. Também tem `@Entity` no domínio.

**Endpoints REST:**
```
POST   /ordens-servico                                                         → 201 + Location + body (AberturaOrdemDeServicoResponse)
PUT    /ordens-servico/{numeroOrdemServico}                                    → 204
DELETE /ordens-servico/{numeroOrdemServico}                                    → 204
GET    /ordens-servico?numeroOrdemServico=&nomeCliente=&placaVeiculo=&documentoCliente= → 200 (prioridade: Execução > Aguardando Aprovação > Diagnóstico > Recebida; exclui Finalizada/Entregue)
GET    /ordens-servico/{numeroOrdemServico}                                    → 200 (StatusOrdemDeServicoResponse: situação canônica + motivo + timestamps)
GET    /ordens-servico/{numeroOrdemServico}/acompanhamento                     → 200 (público, sem autenticação)
POST   /ordens-servico/{numeroOrdemServico}/diagnostico/iniciar                → 204
POST   /ordens-servico/{numeroOrdemServico}/diagnostico/concluir               → 204 (corpo opcional: descrição do serviço + peças usadas — fonte do orçamento)
POST   /ordens-servico/{numeroOrdemServico}/diagnostico/enviar-para-orcamento  → 204 (fluxo único — só dados financeiros; deriva cliente/veículo/funcionário/peças da OS/diagnóstico; cria orçamento + transiciona, atômico)
POST   /ordens-servico/{numeroOrdemServico}/execucao/iniciar                   → 204
POST   /ordens-servico/{numeroOrdemServico}/servico/concluir                   → 204
POST   /ordens-servico/{numeroOrdemServico}/finalizacao                        → 200 (retorna FinalizacaoOrdemDeServicoResponse — legado)
```

**Regras de negócio importantes:**
- Só é possível alterar uma OS com status `OS_ABERTA`
- Não é possível trocar o `Funcionario` responsável via `alterar()`
- Veículo deve pertencer ao cliente informado
- Número da OS gerado como `"OS-" + UUID(8 chars)`
- `POST /ordens-servico` aceita `servicos` e `pecasPrevistas` opcionais; valida existência da peça via `pecainsumo`

### `orcamento`
Orçamento criado a partir do diagnóstico de uma OS. Tem seu próprio ciclo de vida.
- `StatusOrcamento`: `AGUARDANDO_APROVACAO`, `APROVADO`, `REJEITADO`
- Campos calculados: `valorPecas` = soma de (preco × quantidade) de todas as peças; `valorTotal = valorMaoDeObra + valorPecas - desconto`
- Suporta listas de `servicosPropostos` (`List<String>`) e `pecasPrevistas` (`List<PecaOrcamento>`) — armazenadas como `@ElementCollection`
- `PecaOrcamento` é um value object (`@Embeddable`) com: pecaInsumoId, descricao, preco, quantidade
- `AprovarOrcamentoService` reserva as peças no estoque durante a aprovação; se qualquer reserva falhar, libera as já feitas e lança `RegraDeNegocioException`
- **Endpoints:**
  ```
  POST   /orcamentos                       → 201
  GET    /orcamentos/{orcamentoId}         → 200
  GET    /orcamentos?numeroOrcamento=&cpfCliente=&placaVeiculo=  → 200
  PUT    /orcamentos/{orcamentoId}         → 204
  POST   /orcamentos/{orcamentoId}/aprovacao                    → 200 (retorna OrcamentoResponse com status atualizado)
  POST   /orcamentos/{orcamentoId}/rejeicao                     → 204
  DELETE /orcamentos/{orcamentoId}                              → 204
  POST   /integracoes/orcamentos/{numeroOrcamento}/decisao      → 200 (webhook externo — autenticado via X-Webhook-Token; retorna DecisaoOrcamentoResponse)
  ```

**Webhook de decisão (`/integracoes/orcamentos/{n}/decisao`):**
- Autenticado por `WebhookTokenFilter` — header `X-Webhook-Token` com valor de `ORCAMENTO_WEBHOOK_SECRET`
- Payload: `{ "decisao": "APROVADO" | "REJEITADO" }`
- `APROVADO` → reserva peças no estoque + transiciona OS para Execução
- `REJEITADO` → libera reservas + transiciona OS para Finalizada (motivo: ORCAMENTO_RECUSADO)
- Retry idêntico → idempotente (retorna situação atual sem erro)
- Decisão divergente (já decidido com valor diferente) → 400 `RegraDeNegocioException`
- OS não em Aguardando Aprovação → 400

### `pecainsumo`
Controle de estoque de peças e insumos. Não usa UUID — usa `String` como ID.
- Conceitos: `quantidadeEstoque`, `quantidadeReservada`, `getQuantidadeDisponivel()` = estoque − reservado
- `CategoriaPeca`: enum com as categorias das peças (ex: `FREIOS`, `FILTROS`, etc.)
- Use cases (10 no total): `CadastrarPecaInsumo`, `AlterarPecaInsumo`, `ExcluirPecaInsumo`, `BuscarPecaInsumoPorId`, `ListarPecasInsumos`, `AdicionarEstoquePeca`, `RemoverEstoquePeca`, `ReservarPeca`, `LiberarReservaPeca`, `ConsumirPeca`
- Separação JPA correta: `PecaInsumo` (domínio puro) ↔ `PecaInsumoJpaEntity` (infraestrutura) com conversão via `fromDomain()`/`toDomain()`
- **Endpoints:**
  ```
  POST   /pecas-insumos                          → 201
  GET    /pecas-insumos/{id}                     → 200
  GET    /pecas-insumos?marca=&categoria=&possuiReserva=  → 200
  PUT    /pecas-insumos/{id}                     → 204
  DELETE /pecas-insumos/{id}                     → 204
  POST   /pecas-insumos/{id}/adicionar-estoque   → 204
  POST   /pecas-insumos/{id}/remover-estoque     → 204
  POST   /pecas-insumos/{id}/reservar            → 204
  POST   /pecas-insumos/{id}/liberar-reserva     → 204
  POST   /pecas-insumos/{id}/consumir            → 204
  ```

### `notificacao`
Módulo de notificações de mudança de status de OS por e-mail. Sem endpoints REST — opera via eventos de domínio.

- `NotificadorEmail` — porta (interface) com método `enviar(destinatario, assunto, corpo)`
- `NotificadorEmailSpringMail` — adaptador que delega ao Spring Mail (`JavaMailSender`)
- `EnviarNotificacaoStatusOSService` — listener assíncrono (`@Async @EventListener`) de `StatusOrdemDeServicoAlterado`:
  - Busca o cliente pelo `clienteId` do evento
  - Se o cliente tiver `email` preenchido, envia notificação com a nova situação
  - Falha de envio é logada (`log.warn`) mas **não propaga** — a transição de OS não é revertida
  - Se cliente não encontrado, loga warning e encerra silenciosamente
- `@EnableAsync` habilitado em `AsyncConfig` — sem pool personalizado (usa o default do Spring)

Não tem endpoints REST. Não tem repositório próprio — usa `ClienteRepository` do módulo `cliente`.

### `common`
Exceções e tratamento global de erros.
- `RegraDeNegocioException` → HTTP 400
- `RecursoNaoEncontradoException` → HTTP 404
- `IllegalArgumentException` → HTTP 400 (mapeada pelo handler — usado incorretamente em alguns services para "não encontrado")
- `MethodArgumentNotValidException` → HTTP 400 (Bean Validation)
- `HttpMessageNotReadableException` → HTTP 400 (enum inválido no payload, ex: `CategoriaPeca` desconhecida)
- `ApiExceptionHandler` (`@RestControllerAdvice`) trata todos e retorna `ErrorResponse(timestamp, status, message)`

---

## Padrões de código

### Entidades de domínio
Use factory methods estáticos — nunca construtores públicos para criação de negócio:
```java
// Criação (nova entidade)
OrdemDeServico.abrir(id, numero, funcionario, cliente, veiculo)

// Reconstituição (vindo do banco)
OrdemDeServico.reconstituir(id, numero, funcionario, cliente, veiculo, status, iniciadaEm, finalizadaEm, entregueEm)
```

### Repositórios — três camadas obrigatórias
```java
// 1. Interface no domínio
interface OrdemDeServicoRepository {
    void salvar(OrdemDeServico os);
    Optional<OrdemDeServico> buscarPorNumero(String numero);
}

// 2. Adaptador JPA (implementa a interface do domínio)
@Repository
class JpaOrdemDeServicoRepository implements OrdemDeServicoRepository { ... }

// 3. Spring Data (usado pelo adaptador)
interface SpringDataOrdemDeServicoRepository extends JpaRepository<OrdemDeServicoEntity, UUID> { ... }
```

### DTOs
Use `record` para requests e responses. Responses têm factory method `from(DomainObject)`:
```java
public record OrdemDeServicoResponse(...) {
    public static OrdemDeServicoResponse from(OrdemDeServico os) { ... }
}
```

### Exceções
Sempre lance `RegraDeNegocioException` para violações de negócio e `RecursoNaoEncontradoException` para entidades não encontradas. Nunca retorne `null` de repositórios — use `Optional`.

### Transações
```java
@Transactional(readOnly = true)  // nível de classe para repositórios de leitura
@Transactional                   // override em métodos de escrita
```

---

## Testes

### Estratégia geral

Três camadas de teste, cada uma com escopo distinto:

| Camada | Anotação | Banco | Escopo |
|---|---|---|---|
| Unitários (services e domínio) | Nenhuma | Nenhum | Lógica de negócio pura |
| Integração (repositórios JPA) | `@SpringBootTest` + `@ActiveProfiles("integration")` | PostgreSQL real | Persistência e queries |
| Web (controllers) | `@SpringBootTest` + MockMvc | H2 (default) | HTTP, segurança, serialização |

### Perfis de teste

- **Padrão (`./mvnw test`)** — usa `application.yml` com H2 in-memory, Flyway desabilitado, `ddl-auto: create-drop`. Não requer Docker.
- **Integration (`@ActiveProfiles("integration")`)** — usa `application-integration.yml` com PostgreSQL (`localhost:5432/oficina_db`). Requer `docker-compose up -d db`.

### Repositórios de teste (stubs in-memory)

Ficam em `src/test/java/br/com/oficina/support/persistence/`. Cada um implementa a interface de domínio usando `ConcurrentHashMap`:

```
TestClienteRepository        — chave: UUID
TestFuncionarioRepository    — chave: UUID (salvar + buscarPorId)
TestOrcamentoRepository      — chave: UUID, suporta buscarPorFiltros
TestOrdemDeServicoRepository — chave: String (numeroOrdemServico), suporta filtros
TestPecaInsumoRepository     — chave: String (id)
TestVeiculoRepository        — chave: String (placa normalizada), 7 filtros
```

Padrão de implementação — UUID gerado pelo próprio stub se ainda nulo:
```java
public Cliente salvar(Cliente cliente) {
    Cliente clientePersistido = cliente.getId() == null
        ? Cliente.reconstituir(UUID.randomUUID(), cliente.getNome(), ...)
        : cliente;
    clientes.put(clientePersistido.getId(), clientePersistido);
    return clientePersistido;
}
```

### Testes unitários de services

Services instanciados com construtor — sem Spring, sem Mockito na maioria dos casos. Cada teste cria instâncias frescas (sem `@BeforeEach` compartilhado):

```java
@Test
void deveCadastrarCliente() {
    TestClienteRepository repository = new TestClienteRepository();
    CadastrarClienteService service = new CadastrarClienteService(repository);

    var clienteId = service.cadastrarCliente(new CadastrarClienteCommand("Maria", "12345678901", TipoCliente.PF));

    assertNotNull(clienteId);
    assertEquals("Maria", repository.buscarPorId(clienteId).orElseThrow().getNome());
}
```

Quando o service depende de múltiplos repositórios, pré-popule os stubs antes de chamar o service:

```java
TestOrcamentoRepository orcamentoRepo = new TestOrcamentoRepository();
TestClienteRepository clienteRepo = new TestClienteRepository();
TestPecaInsumoRepository pecaRepo = new TestPecaInsumoRepository();

clienteRepo.salvar(Cliente.reconstituir(clienteId, "Joao", "12345678901", TipoCliente.PF));
pecaRepo.salvar(new PecaInsumo(PECA_ID, "Pastilha", "Bosch", new BigDecimal("250.00"), 10, 0, "REF-001", CategoriaPeca.FREIOS));

CadastrarNovoOrcamentoService service = new CadastrarNovoOrcamentoService(orcamentoRepo, clienteRepo, pecaRepo);
```

### Testes de modelo de domínio

Use `reconstituir()` para criar objetos em testes (simula leitura do banco). Para criação, use `abrir()` ou a factory correspondente:

```java
Cliente cliente = Cliente.reconstituir(
    UUID.fromString("11111111-1111-1111-1111-111111111111"),
    "Maria", "12345678901", TipoCliente.PF);
```

Para testes de máquina de estados, encadeie as transições e verifique o estado final:

```java
OrdemDeServico os = novaOrdem("OS-001");
os.iniciarDiagnostico();
os.concluirDiagnostico();
os.enviarParaOrcamento();
os.finalizar();

assertEquals(StatusOrdemDeServico.OS_FINALIZADA, os.getStatus());
assertNotNull(os.getFinalizadaEm());
```

Use `UUID.nameUUIDFromBytes()` para IDs determinísticos em helpers de teste:

```java
private OrdemDeServico novaOrdem(String numero) {
    return OrdemDeServico.abrir(
        UUID.nameUUIDFromBytes(("ordem-" + numero).getBytes()),
        numero,
        Funcionario.reconstituir(UUID.nameUUIDFromBytes(("func-" + numero).getBytes()), "Joao", null),
        ...);
}
```

### Testes de exceção

```java
// Violação de negócio
RegraDeNegocioException ex = assertThrows(
    RegraDeNegocioException.class,
    () -> service.reservarPeca(new ReservarPecaCommand("peca-1", 0)));
assertEquals("A quantidade a ser reservada deve ser maior que zero", ex.getMessage());

// Recurso não encontrado
RecursoNaoEncontradoException ex = assertThrows(
    RecursoNaoEncontradoException.class,
    () -> service.cadastrarNovoOrcamento(command));
assertEquals("Cliente nao encontrado para o identificador informado.", ex.getMessage());
```

Sempre verifique a **mensagem exata** da exceção — ela é parte do contrato.

### Testes de integração (repositórios JPA)

```java
@SpringBootTest
@ActiveProfiles("integration")
@Transactional          // rollback automático após cada teste
class JpaClienteRepositoryIntegrationTest {

    @Autowired
    private JpaClienteRepository repository;

    @Test
    void devePersistirBuscarAtualizarEExcluirClienteNoBanco() { ... }
}
```

Quando a OS/Orçamento depende de um Veículo pré-existente, injete o `SpringData*Repository` diretamente para criar o dado auxiliar (o `@Transactional` garante o rollback de tudo junto).

### Testes de controller (camada web)

```java
@SpringBootTest
class ClienteControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SpringDataClienteRepository clienteRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        clienteRepository.deleteAll();   // limpa estado entre testes
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }
}
```

Padrão de requisição — sempre inclua `.with(user("tester"))` e `.with(csrf())` em rotas protegidas:

```java
mockMvc.perform(post("/clientes")
        .with(user("tester"))
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            { "nome": "Maria", "cpfOuCnpj": "12345678901", "tipoCliente": "PF" }
            """))
    .andExpect(status().isCreated())
    .andExpect(header().string("Location", matchesPattern("http://localhost/clientes/.+")));
```

Verificação de corpo de resposta com JSONPath:

```java
mockMvc.perform(get("/clientes/" + clienteId).with(user("tester")))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.nome").value("Maria"))
    .andExpect(jsonPath("$.tipoCliente").value("PF"));

// Para listas:
.andExpect(jsonPath("$[*].nome", hasItem("Maria")));
```

Teste de erro:

```java
mockMvc.perform(get("/clientes/" + UUID.randomUUID()).with(user("tester")))
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.message").value("Cliente nao encontrado para o identificador informado."));
```

### Nomenclatura dos testes

Todos os nomes de métodos em português, começando com `deve`, descrevendo o comportamento esperado:

```
deveCadastrarCliente()
deveFalharAoCadastrarPessoaFisicaComCpfInvalido()
devePersistirBuscarAtualizarEExcluirClienteNoBanco()
deveRetornarNotFoundQuandoClienteNaoExiste()
deveAdicionarRemoverReservarELiberarQuantidadeDaPeca()
```

### Testes de fluxo (flow tests)

Um único teste que exercita múltiplos services em sequência, compartilhando um repositório in-memory:

```java
@Test
void deveCadastrarAlterarListarEExcluirPecaInsumo() {
    TestPecaInsumoRepository repository = new TestPecaInsumoRepository();

    new CadastrarPecaInsumoService(repository).cadastrarPecaInsumo(commandCadastro);
    PecaInsumo cadastrada = repository.buscarTodos().get(0);
    assertEquals("Filtro de ar", cadastrada.getDescricao());

    new AlterarPecaInsumoService(repository).alterarPecaInsumo(commandAlterar);
    assertEquals("Filtro de ar esportivo", repository.buscarPorId(cadastrada.getId()).orElseThrow().getDescricao());

    new ExcluirPecaInsumoService(repository).excluirPecaInsumo(commandExcluir);
    assertTrue(repository.buscarPorId(cadastrada.getId()).isEmpty());
}
```

### Mockito — quando usar

Use Mockito apenas para testar adaptadores JPA, onde o objetivo é verificar a interação com o `SpringData*Repository`:

```java
@Mock
private SpringDataVeiculoRepository springDataRepository;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
    jpaRepository = new JpaVeiculoRepository(springDataRepository);
}

@Test
void deveSalvarVeiculoNoSpringData() {
    jpaRepository.salvar(veiculo);
    verify(springDataRepository).save(veiculo);
}
```

Para services e modelo de domínio, prefira sempre os stubs in-memory (`Test*Repository`).

### Cobertura mínima obrigatória (JaCoCo — bloqueia o build se não atingida)

| Módulo | Cobertura |
|---|---|
| `br.com.oficina.veiculo/*` | 80% linhas e branches |
| `br.com.oficina.ordemservico/*` | 80% linhas e branches |
| `br.com.oficina.orcamento/*` | 80% linhas e branches |
| `br.com.oficina.cliente/*` | 80% linhas e branches |
| `br.com.oficina.notificacao/*` | 80% linhas e branches |
| `br.com.oficina.pecainsumo/domain/model` | 80% linhas |
| `br.com.oficina.pecainsumo/application/service` | 80% linhas e branches |

---

## Banco de dados

Migrações Flyway em `src/main/resources/db/migration/`. Sempre criar novo arquivo `V{N}__descricao.sql`.

Schema principal (simplificado):
```
usuarios          — auth (BIGSERIAL PK)
clientes          — UUID PK
veiculos          — UUID PK, FK cliente_id
funcionarios      — UUID PK
ordens_de_servico — UUID PK, desnormalizado (dados de cliente/veiculo/funcionario copiados)
orcamentos        — UUID PK, FK implícita para OS
orcamento_servicos_propostos — tabela associativa
orcamento_pecas_previstas    — tabela associativa
pecas_insumos     — String PK (não UUID)
```

O JPA usa `ddl-auto: none` — o schema vem exclusivamente do Flyway. `FlywayConfig` garante que o Flyway execute antes do `EntityManagerFactory` inicializar.

---

## O que NÃO fazer

Antipadrões encontrados no código existente. Não repita-os em código novo.
(As mesmas proibições estão codificadas como regras normativas na constituição.)

### 1. Usar `IllegalArgumentException` para "recurso não encontrado"

`CriarNovaOrdemDeServicoService` e `AlterarOrdemDeServicoService` lançam `IllegalArgumentException` quando um Cliente, Veículo ou Funcionário não é encontrado. O `ApiExceptionHandler` mapeia essa exceção para **HTTP 400**, mas o correto é **HTTP 404**.

Use sempre `RecursoNaoEncontradoException` para entidades não encontradas (como `CadastrarNovoOrcamentoService` faz corretamente):
```java
// Errado (gera 400 para "não encontrado")
.orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado"));

// Certo (gera 404)
.orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado"));
```

### 2. Expor construtores públicos em entidades de domínio

`Orcamento` possui dois construtores públicos com até 18 parâmetros, em vez de seguir o padrão de factory methods estáticos adotado por `OrdemDeServico`, `Cliente` e `Veiculo`. Construtores públicos em entidades de domínio permitem criar instâncias em estado inválido e dificultam a leitura do site de criação.

```java
// Errado — construtor público de 18 parâmetros em Orcamento
public Orcamento(String numeroOrcamento, String ordemDeServicoId, ...)

// Certo — factory method estático com nome semântico (padrão do projeto)
public static Orcamento abrir(String numeroOrcamento, String ordemDeServicoId, ...)
```

### 3. Transições de estado sem validação de guarda

`Orcamento.aprovar()` e `Orcamento.rejeitar()` não verificam o status atual antes de transicionar. Qualquer orçamento pode ser aprovado ou rejeitado independente do estado. Contrast com `OrdemDeServico`, que lança `RegraDeNegocioException` em toda transição inválida.

```java
// Errado — sem guarda em Orcamento
public void aprovar() {
    this.status = StatusOrcamento.APROVADO;
}

// Certo — com guarda (padrão do projeto)
public void aprovar() {
    if (status != StatusOrcamento.AGUARDANDO_APROVACAO) {
        throw new RegraDeNegocioException("Orcamento so pode ser aprovado quando aguardando aprovacao");
    }
    this.status = StatusOrcamento.APROVADO;
}
```

### 4. Anotações JPA no modelo de domínio

`OrdemDeServico` carrega `@Entity`, `@Column`, `@Enumerated` diretamente no aggregate root. Isso acopla o domínio ao JPA — viola o princípio central da arquitetura hexagonal do projeto.

O módulo `pecainsumo` resolve isso corretamente: `PecaInsumo` é uma classe Java pura e `PecaInsumoJpaEntity` é o objeto de persistência separado, com conversão via `fromDomain()` / `toDomain()`. Ao criar novos módulos ou refatorar, prefira esse padrão.

### 5. Geração de UUID dentro do construtor do domínio

`PecaInsumo` chama `UUID.randomUUID()` dentro do próprio construtor de criação. Isso torna o objeto não-determinístico, dificulta testes e é inconsistente com os demais módulos, onde a geração de ID ocorre no service ou é delegada ao JPA.

```java
// Errado — UUID gerado no construtor do domínio
public PecaInsumo(String descricao, ...) {
    this(UUID.randomUUID().toString(), descricao, ...);
}

// Certo — ID gerado no service ou pelo @GeneratedValue do JPA
```

### 6. Tipos inconsistentes para IDs entre módulos

`Orcamento` declara `ordemDeServicoId` e `funcionarioId` como `String`, enquanto todo o restante do projeto usa `UUID` para identificadores. Isso força conversões explícitas e esconde erros de parsing em tempo de execução. Ao adicionar campos de referência entre módulos, use `UUID`.

### 7. Passar `null` explicitamente em overloads de `reconstituir`

`Orcamento.reconstituir(id, numeroOrcamento, ...)` (sem `clienteId`) delega para o overload com `clienteId` passando `null` como literal. Isso indica que o campo `clienteId` é opcional mas o tipo não comunica isso — use `Optional<UUID>` ou consolide os overloads.

### 8. ~~`POST /ordens-servico` retornando `202 Accepted` sem motivo~~ ✓ Corrigido

`POST /ordens-servico` agora retorna **201 Created** com header `Location` e body `AberturaOrdemDeServicoResponse`. O antipadrão original (202) foi corrigido.

---

## O que está pendente / incompleto

- **Refatoração de domínio puro** (`T008–T010`) — `OrdemDeServico` e `Funcionario` ainda têm `@Entity` diretamente no aggregate root. A separação em `OrdemDeServicoJpaEntity` (padrão `pecainsumo`) está adiada conscientemente. Lock otimista (`@Version`) para concorrência de transições (`T010a`) também pendente.
- **Paginação** — nenhum endpoint tem paginação ainda
- **TODO no AuthController** — comentário `// TODO refatorar controller com boas práticas` indica intenção de reestruturar o módulo auth para seguir a arquitetura hexagonal
- **Validação de email** — campo `email` em `Cliente` é string livre sem validação de formato; o sistema simplesmente ignora clientes sem email ao notificar
