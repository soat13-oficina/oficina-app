# Oficina API

Backend monolítico para gestão de oficina mecânica. **Java 21 · Spring Boot 4 · PostgreSQL · Arquitetura Hexagonal.**

> **Para agentes LLM**: este arquivo é o índice de navegação do projeto. Cada seção aponta
> ao README do domínio responsável — leia apenas o que for relevante à tarefa. Os READMEs
> de domínio contêm achados críticos, regras de negócio detalhadas e referências de arquivo.

---

## Índice Rápido

| Seção | Descrição |
|---|---|
| [Mapa de Domínios](#mapa-de-domínios) | Responsabilidade, endpoints e link para cada domínio |
| [Ciclo de Vida Principal](#ciclo-de-vida-principal) | Fluxo completo da OS do início à entrega |
| [Regras de Negócio Transversais](#regras-de-negócio-transversais) | Regras que atravessam múltiplos domínios |
| [Índice de Casos de Uso](#índice-de-casos-de-uso) | Todos os use cases → domínio → endpoint |
| [Índice de Endpoints REST](#índice-de-endpoints-rest) | Todos os endpoints da API |
| [Mapa de Dependências](#mapa-de-dependências) | Quem consome quem |
| [Achados Críticos Consolidados](#achados-críticos-consolidados) | Todos os CRIs de todos os domínios |
| [Tratamento de Exceções](#tratamento-de-exceções) | Exceções globais e mapeamento HTTP |
| [Padrões de Código](#padrões-de-código) | Arquitetura, nomenclatura, testes |
| [Schema do Banco](#schema-do-banco) | Tabelas e migrations Flyway |
| [Setup e Operação](#setup-e-operação) | Docker, build, testes, Swagger |

---

## Mapa de Domínios

| Domínio | Responsabilidade | Endpoints | Detalhes |
|---|---|---|---|
| [`auth`](src/main/java/br/com/oficina/auth/README.md) | Autenticação JWT stateless; login, registro, `SecurityConfig`, filtros JWT e webhook | 2 públicos | [→ README](src/main/java/br/com/oficina/auth/README.md) |
| [`cliente`](src/main/java/br/com/oficina/cliente/README.md) | CRUD de clientes PF/PJ; validação CPF/CNPJ (11/14 dígitos); busca por nome e documento | 5 | [→ README](src/main/java/br/com/oficina/cliente/README.md) |
| [`veiculo`](src/main/java/br/com/oficina/veiculo/README.md) | CRUD de veículos; placa no formato antigo e Mercosul; filtros dinâmicos via JPA Specification | 4 | [→ README](src/main/java/br/com/oficina/veiculo/README.md) |
| [`ordemservico`](src/main/java/br/com/oficina/ordemservico/README.md) | **Domínio central.** Ciclo de vida da OS; máquina de estados com 12 status; snapshot de dados; métricas | 15+ | [→ README](src/main/java/br/com/oficina/ordemservico/README.md) |
| [`orcamento`](src/main/java/br/com/oficina/orcamento/README.md) | Orçamentos gerados a partir do diagnóstico; cálculo de valor; decisão via webhook externo | 1 REST + 1 webhook | [→ README](src/main/java/br/com/oficina/orcamento/README.md) |
| [`pecainsumo`](src/main/java/br/com/oficina/pecainsumo/README.md) | Estoque de peças; reserva e consumo controlados; **padrão arquitetural de referência** (domínio puro + JpaEntity separada) | 5 | [→ README](src/main/java/br/com/oficina/pecainsumo/README.md) |
| [`notificacao`](src/main/java/br/com/oficina/notificacao/README.md) | Envia emails ao cliente quando o status da OS muda; listener assíncrono de evento; sem endpoints REST | 0 (orientado a evento) | [→ README](src/main/java/br/com/oficina/notificacao/README.md) |
| `common` | `ApiExceptionHandler` centralizado; `RecursoNaoEncontradoException`; `RegraDeNegocioException` | — | `common/infrastructure/web/ApiExceptionHandler.java` |

---

## Ciclo de Vida Principal

O fluxo de negócio central atravessa 5 domínios nesta ordem:

```
[cliente cadastrado] + [veiculo cadastrado]
            |
            | POST /ordens-servico
            ▼
      [OS_ABERTA]  (ordemservico)
            |
            | POST /{num}/diagnostico/iniciar
            ▼
[DIAGNOSTICO_EM_ANDAMENTO]
            |
            | POST /{num}/diagnostico/concluir
            ▼
  [DIAGNOSTICO_CONCLUIDO]
            |
            | POST /{num}/diagnostico/enviar-para-orcamento
            |   → cria Orcamento (orcamento)
            ▼
  [AGUARDANDO_APROVACAO]
            |
            | POST /integracoes/orcamentos/{num}/decisao  (webhook externo)
            |   → APROVADO → [ORCAMENTO_APROVADO]
            |   → REJEITADO → [OS_FINALIZADA / ORCAMENTO_RECUSADO]
            |
            | POST /{num}/execucao/iniciar
            ▼
  [SERVICO_EM_ANDAMENTO]
            |
            | POST /{num}/servico/concluir
            |   → ConsumirPeca para cada peça do orçamento (pecainsumo)
            ▼
     [OS_FINALIZADA]
            |
            | POST /{num}/entrega
            ▼
        [ENTREGUE]
```

**Em cada transição de status** → `StatusOrdemDeServicoAlterado` é publicado → `notificacao`
envia email ao cliente de forma assíncrona (fire-and-forget).

**Fluxo alternativo de finalização direta** (sem ciclo de aprovação completo):  
`DIAGNOSTICO_CONCLUIDO` → `POST /{num}/orcamento/enviar-aprovacao` → `[ORCAMENTO_GERADO]`
→ `POST /{num}/finalizacao` → `[OS_FINALIZADA]`

**Visão simplificada para o cliente** (`SituacaoOrdemDeServico`):
`RECEBIDA → DIAGNOSTICO → AGUARDANDO_APROVACAO → EXECUCAO → FINALIZADA → ENTREGUE`

---

## Regras de Negócio Transversais

### Identidade e Autenticação

| Regra | Detalhe |
|---|---|
| Toda rota requer JWT | Header `Authorization: Bearer <token>`; token expira em 24h |
| Webhook de orçamento sem JWT | `POST /integracoes/orcamentos/*/decisao` usa `X-Webhook-Token` (header dedicado) |
| Usuário identificado por email | Subject do JWT; senhas com BCrypt; sem roles/RBAC implementado |

### Identificadores

| Domínio | Tipo de ID | Coluna |
|---|---|---|
| `auth.Usuario` | `Long` (BIGSERIAL) | `id` — **exceção** ao padrão UUID |
| Todos os demais | `UUID` gerado por `GenerationType.UUID` | `id` |
| `PecaInsumo` | `String` (UUID como String) | `id` — gerado via `UUID.randomUUID().toString()` |
| `Orcamento` | `String` (UUID como String) | `numero` |
| `OrdemDeServico` | `String` (UUID como String) | `numero` |

### Snapshots Imutáveis

Ao criar uma `OrdemDeServico` e um `Orcamento`, os dados de cliente, veículo e funcionário
são **copiados** e permanecem imutáveis. Alterações posteriores nos cadastros não retroagem.

| Snapshot em `OrdemDeServico` | Snapshot em `Orcamento` |
|---|---|
| `funcionarioNome`, `funcionarioRegistro` | `clienteNome`, `clienteCpf` |
| `clienteNome`, `clienteDocumento`, `clienteTipo` | `placaVeiculo`, `marcaVeiculo`, `modeloVeiculo` |
| `veiculoPlaca`, `veiculoMarca`, `veiculoModelo`, `veiculoAno` | — |

### Validações de Domínio

| Entidade | Regra |
|---|---|
| `Cliente` | Nome obrigatório; CPF = 11 dígitos; CNPJ = 14 dígitos; tipo e documento informados juntos; CPF/CNPJ único |
| `Veiculo` | Placa: formato antigo `^[A-Z]{3}\d{4}$` ou Mercosul `^[A-Z]{3}\d[A-Z]\d{2}$`; normalizada (uppercase, sem espaço/hífen); placa única; placa imutável após cadastro |
| `OrdemDeServico` | Transições de status guardadas (`RegraDeNegocioException` se inválida) |
| `Orcamento` | `valorTotal = valorMaoDeObra + valorPecas - desconto`; decisão idempotente; conflito de decisão → 400 |
| `PecaInsumo` | `reservar`: `quantidadeDisponivel >= qtd`; `consumir`: `quantidadeReservada >= qtd` |

### Consistência Referencial (FKs no Banco)

| FK | Tabela origem | Tabela destino | Risco |
|---|---|---|---|
| `fk_veiculos_cliente_id` | `veiculos.cliente_id` | `clientes.id` | Excluir cliente com veículo → 500 (não tratado) |
| `fk_ordens_de_servico_cliente_id` | `ordens_de_servico.cliente_id` | `clientes.id` | Excluir cliente com OS → 500 |
| `fk_ordens_de_servico_veiculo_id` | `ordens_de_servico.veiculo_id` | `veiculos.id` | Excluir veículo com OS → 500 |

---

## Índice de Casos de Uso

| Caso de Uso | Domínio | Endpoint | Tipo |
|---|---|---|---|
| Login | `auth` | `POST /api/auth/login` | Público |
| Registro | `auth` | `POST /api/auth/register` | Público |
| Cadastrar cliente | `cliente` | `POST /clientes` | Autenticado |
| Alterar cliente | `cliente` | `PUT /clientes/{id}` | Autenticado |
| Excluir cliente | `cliente` | `DELETE /clientes/{id}` | Autenticado |
| Consultar cliente | `cliente` | `GET /clientes/{id}` | Autenticado |
| Pesquisar clientes | `cliente` | `GET /clientes?termo=` | Autenticado |
| Cadastrar veículo | `veiculo` | `POST /veiculos` | Autenticado |
| Alterar veículo | `veiculo` | `PUT /veiculos/{placa}` | Autenticado |
| Excluir veículo | `veiculo` | `DELETE /veiculos/{placa}` | Autenticado |
| Consultar veículos | `veiculo` | `GET /veiculos` | Autenticado |
| Criar OS | `ordemservico` | `POST /ordens-servico` | Autenticado |
| Alterar OS | `ordemservico` | `PUT /ordens-servico/{num}` | Autenticado |
| Excluir OS | `ordemservico` | `DELETE /ordens-servico/{num}` | Autenticado |
| Consultar OS | `ordemservico` | `GET /ordens-servico` | Autenticado |
| Consultar OS por número | `ordemservico` | `GET /ordens-servico/{num}` | Autenticado |
| Acompanhar OS (cliente) | `ordemservico` | `GET /ordens-servico/{num}/acompanhamento` | Autenticado |
| Métricas — tempo médio | `ordemservico` | `GET /ordens-servico/metricas/tempo-medio` | Autenticado |
| Iniciar diagnóstico | `ordemservico` | `POST /ordens-servico/{num}/diagnostico/iniciar` | Autenticado |
| Concluir diagnóstico | `ordemservico` | `POST /ordens-servico/{num}/diagnostico/concluir` | Autenticado |
| Enviar diagnóstico para orçamento | `ordemservico` | `POST /ordens-servico/{num}/diagnostico/enviar-para-orcamento` | Autenticado |
| Enviar para aprovação | `ordemservico` | `POST /ordens-servico/{num}/orcamento/enviar-aprovacao` | Autenticado |
| Iniciar execução | `ordemservico` | `POST /ordens-servico/{num}/execucao/iniciar` | Autenticado |
| Concluir serviço | `ordemservico` | `POST /ordens-servico/{num}/servico/concluir` | Autenticado |
| Finalizar OS | `ordemservico` | `POST /ordens-servico/{num}/finalizacao` | Autenticado |
| Entregar ao cliente | `ordemservico` | `POST /ordens-servico/{num}/entrega` | Autenticado |
| Consultar orçamento | `orcamento` | `GET /orcamentos/{num}` | Autenticado |
| Decidir orçamento (webhook) | `orcamento` | `POST /integracoes/orcamentos/{num}/decisao` | X-Webhook-Token |
| Cadastrar peça | `pecainsumo` | `POST /pecas-insumos` | Autenticado |
| Alterar peça | `pecainsumo` | `PUT /pecas-insumos/{id}` | Autenticado |
| Excluir peça | `pecainsumo` | `DELETE /pecas-insumos/{id}` | Autenticado |
| Buscar peça | `pecainsumo` | `GET /pecas-insumos/{id}` | Autenticado |
| Listar peças | `pecainsumo` | `GET /pecas-insumos` | Autenticado |
| Reservar peça (interno) | `pecainsumo` | — | Chamado por `ordemservico` |
| Consumir peça (interno) | `pecainsumo` | — | Chamado por `ordemservico` |
| Criar orçamento (interno) | `orcamento` | — | Chamado por `ordemservico` |
| Notificar cliente (interno) | `notificacao` | — | Listener de `StatusOrdemDeServicoAlterado` |

---

## Índice de Endpoints REST

### Auth
```
POST /api/auth/login        # Público
POST /api/auth/register     # Público
```

### Clientes
```
GET    /clientes             # ?termo=
POST   /clientes
GET    /clientes/{id}
PUT    /clientes/{id}
DELETE /clientes/{id}
```

### Veículos
```
GET    /veiculos             # ?placa= &ano= &marca= &fabricante= &potencia= &cambio= &tipo=
POST   /veiculos
PUT    /veiculos/{placa}
DELETE /veiculos/{placa}
```

### Ordens de Serviço
```
GET    /ordens-servico                                       # ?numero= &status= &situacao= &clienteId= &veiculoPlaca=
POST   /ordens-servico
GET    /ordens-servico/metricas/tempo-medio
GET    /ordens-servico/{numero}
PUT    /ordens-servico/{numero}
DELETE /ordens-servico/{numero}
GET    /ordens-servico/{numero}/acompanhamento
POST   /ordens-servico/{numero}/diagnostico/iniciar
POST   /ordens-servico/{numero}/diagnostico/concluir
POST   /ordens-servico/{numero}/diagnostico/enviar-para-orcamento
POST   /ordens-servico/{numero}/orcamento/enviar-aprovacao
POST   /ordens-servico/{numero}/execucao/iniciar
POST   /ordens-servico/{numero}/servico/concluir
POST   /ordens-servico/{numero}/finalizacao
POST   /ordens-servico/{numero}/entrega
```

### Orçamentos
```
GET  /orcamentos/{numeroOrcamento}
POST /integracoes/orcamentos/{numeroOrcamento}/decisao   # X-Webhook-Token (sem JWT)
```

### Peças e Insumos
```
GET    /pecas-insumos
POST   /pecas-insumos
GET    /pecas-insumos/{id}
PUT    /pecas-insumos/{id}
DELETE /pecas-insumos/{id}
```

---

## Mapa de Dependências

Leia como "linha consome coluna":

| Consumidor ↓ / Provedor → | `auth` | `cliente` | `veiculo` | `ordemservico` | `orcamento` | `pecainsumo` | `notificacao` |
|---|---|---|---|---|---|---|---|
| `auth` | — | — | — | — | — | — | — |
| `cliente` | — | — | — | — | — | — | — |
| `veiculo` | — | ✓ (valida proprietário) | — | — | — | — | — |
| `ordemservico` | — | ✓ (snapshot) | ✓ (snapshot) | — | ✓ (cria + consulta) | ✓ (consume) | — |
| `orcamento` | — | — | — | ✓ (publica evento) | — | — | — |
| `notificacao` | — | ✓ (busca email) | — | ✓ (escuta evento) | — | — | — |
| `pecainsumo` | — | — | — | — | — | — | — |

**Evento assíncrono**: `ordemservico` publica `StatusOrdemDeServicoAlterado` →
`notificacao` escuta via `@EventListener @Async` → email ao cliente.

---

## Achados Críticos Consolidados

Achados que bloqueiam funcionalidades ou causam HTTP 500 em produção. Leia o README do
domínio para contexto completo e correção sugerida.

| ID | Domínio | Componente afetado | Impacto em produção |
|---|---|---|---|
| [auth/CRI-001](src/main/java/br/com/oficina/auth/README.md#cri-001-post-apiauthregister-retorna-http-500-para-falhas-genéricas) | `auth` | `AuthController#register` | Falhas de registro retornam HTTP 500 sem mensagem nem log |
| [auth/CRI-002](src/main/java/br/com/oficina/auth/README.md#cri-002-ausência-de-validação-de-entrada-no-authcontroller) | `auth` | `LoginRequest` | Email/senha nulos chegam ao Spring Security → comportamento indefinido |
| [cliente/CRI-001](src/main/java/br/com/oficina/cliente/README.md#cri-001-hard-delete-sem-verificação-de-integridade-referencial) | `cliente` | `ExcluirClienteService` | Excluir cliente com veículo/OS → `DataIntegrityViolationException` → HTTP 500 |
| [cliente/CRI-002](src/main/java/br/com/oficina/cliente/README.md#cri-002-busca-de-unicidade-de-documento-em-memória) | `cliente` | `JpaClienteRepository#buscarPorDocumento` | Unicidade de CPF/CNPJ verificada em memória → race condition + performance |
| ✅ [veiculo/CRI-001](src/main/java/br/com/oficina/veiculo/README.md) | `veiculo` | `ExcluirVeiculoService` | **RESOLVIDO** (spec 003) — exclusão com OS vinculada → HTTP 400 |
| [notificacao/CRI-001](src/main/java/br/com/oficina/notificacao/README.md#cri-001-sem-retry-e-sem-persistência-de-notificações-pendentes) | `notificacao` | `EnviarNotificacaoStatusOSService` | Falhas SMTP descartam notificações permanentemente; sem retry |
| [orcamento/CRI-001](src/main/java/br/com/oficina/orcamento/README.md#cri-001-ausência-de-validação-de-transição-antes-de-enviarparaaprovação) | `orcamento` | `Orcamento#enviarParaAprovacao` | Webhook chega antes do envio → 400 sem contexto útil |
| [orcamento/CRI-002](src/main/java/br/com/oficina/orcamento/README.md#cri-002-propagação-de-evento-na-decisão-sem-garantia-de-atomicidade) | `orcamento` | `DecidirOrcamentoExternamenteService` | Orçamento persistido sem garantia de propagação do evento → inconsistência |
| [pecainsumo/CRI-001](src/main/java/br/com/oficina/pecainsumo/README.md#cri-001-buscaporid-lança-entitynotfoundexception-não-tratada-pelo-apiexceptionhandler) | `pecainsumo` | `PecaInsumoController#buscarPorId` | `EntityNotFoundException` não mapeada → HTTP 500 em vez de 404 |
| [pecainsumo/CRI-002](src/main/java/br/com/oficina/pecainsumo/README.md#cri-002-reservarpecaservice-e-consumirpecaservice-usam-exceção-errada-para-não-encontrado) | `pecainsumo` | `ReservarPecaService`, `ConsumirPecaService` | `RegraDeNegocioException` para "não encontrado" → HTTP 400 em vez de 404 |
| [pecainsumo/CRI-003](src/main/java/br/com/oficina/pecainsumo/README.md#cri-003-ausência-de-verificação-de-reservas-ao-excluir-peça) | `pecainsumo` | `ExcluirPecaInsumoService` | Peça excluída com reserva ativa → OS não pode ser finalizada |
| [ordemservico/CRI-001](src/main/java/br/com/oficina/ordemservico/README.md#cri-001-ausência-de-logging-em-todos-os-services-do-módulo) | `ordemservico` | Todos os services | Zero rastreabilidade em produção; viola Princípio V da constituição |
| [ordemservico/CRI-002](src/main/java/br/com/oficina/ordemservico/README.md#cri-002-filtragem-de-os-em-memória-sem-paginação) | `ordemservico` | `ConsultarOrdensDeServicoService` | Carrega toda a tabela em memória → `OutOfMemoryError` com volume |
| [ordemservico/CRI-003](src/main/java/br/com/oficina/ordemservico/README.md#cri-003-status-técnicos-sem-transições-implementadas--lacuna-de-ciclo-de-vida) | `ordemservico` | `StatusOrdemDeServico` | 4 status sem transições → OS podem ficar presas sem saída na API |
| [ordemservico/CRI-004](src/main/java/br/com/oficina/ordemservico/README.md#cri-004-falta-de-validação-de-veiculoid-pertencente-ao-clienteid-na-criação-da-os) | `ordemservico` | `CriarNovaOrdemDeServicoService` | OS pode ser criada com veículo de outro cliente |

---

## Tratamento de Exceções

**Handler central**: `common/infrastructure/web/ApiExceptionHandler.java`

| Exceção | HTTP | Observação |
|---|---|---|
| `RecursoNaoEncontradoException` | 404 | Padrão para entidade não encontrada |
| `RegraDeNegocioException` | 400 | Violação de regra de negócio |
| `MethodArgumentNotValidException` | 400 | Bean Validation (`@Valid`) |
| `HttpMessageNotReadableException` | 400 | Enum inválido — extrai nome do tipo dinamicamente |
| `MethodArgumentTypeMismatchException` | 400 | Path param inválido (ex.: UUID mal-formado) |
| `IllegalArgumentException` | 400 | — |
| `DataIntegrityViolationException` | **NÃO MAPEADO** → 500 | FK violation ao excluir cliente/veículo com registros |
| `EntityNotFoundException` | **NÃO MAPEADO** → 500 | Lançado em `PecaInsumoController#buscarPorId` |

---

## Padrões de Código

### Arquitetura Hexagonal (por módulo)

```
{modulo}/
├── domain/
│   ├── model/          # Aggregate root + value objects (sem JPA)
│   ├── repository/     # Port (interface)
│   └── event/          # Eventos de domínio
├── application/
│   ├── usecase/        # Interfaces dos casos de uso
│   ├── service/        # Implementações
│   ├── command/        # Inputs de escrita
│   └── query/          # Inputs de leitura
└── infrastructure/
    ├── persistence/    # JpaEntity + JpaRepository (adapter)
    └── web/            # Controller + Request/Response
```

**Módulo de referência**: `pecainsumo` — único com domínio puro (sem JPA) e `PecaInsumoJpaEntity`
separada com `fromDomain()` / `toDomain()`. Constituição (Princípio I) cita este como
o padrão correto.

**Dívida**: `cliente`, `veiculo`, `ordemservico`, `orcamento` têm anotações JPA diretamente
no aggregate root — violam o Princípio I.

### Exceções de Domínio

| Exceção | Uso correto |
|---|---|
| `RecursoNaoEncontradoException` | Entidade não encontrada por ID/chave |
| `RegraDeNegocioException` | Violação de regra em entidade existente (transição inválida, estoque insuficiente, etc.) |

### Logging (Princípio V da Constituição)

SLF4J com parâmetros nomeados: `log.info("Operacao. campo={}", valor)`. Obrigatório na
entrada e saída de cada operação de negócio.  
**`ordemservico` não possui logging** — dívida técnica explícita.

### Testes (3 camadas)

| Camada | Anotação | Banco |
|---|---|---|
| Unitário | Nenhuma (pure Java) | In-memory stubs |
| Controller/Web | `@SpringBootTest` + MockMvc | H2 |
| Integração | `@SpringBootTest @ActiveProfiles("integration")` | PostgreSQL real |

Cobertura mínima: **80% linhas e branches** via JaCoCo (build falha abaixo do limite).

---

## Schema do Banco

14 migrations Flyway (`V1` → `V14`). Tabelas principais:

| Tabela | Domínio | Observação |
|---|---|---|
| `usuarios` | `auth` | `id BIGSERIAL` (único não-UUID) |
| `clientes` | `cliente` | `cpf_ou_cnpj` sem índice UNIQUE (dívida — ver cliente/CRI-002) |
| `veiculos` | `veiculo` | `placa UNIQUE`; FK para `clientes` |
| `ordens_de_servico` | `ordemservico` | FK para `clientes` e `veiculos`; `servicos` e `pecas_previstas` em tabelas de coleção |
| `orcamentos` | `orcamento` | Snapshot de cliente e veículo; `pecas_orcamento` como `@ElementCollection` |
| `pecas_insumos` | `pecainsumo` | `quantidade_estoque`, `quantidade_reservada` |
| `funcionarios` | `ordemservico` | Referenciada por snapshot na OS; sem endpoint próprio de CRUD nesta API |

---

## Setup e Operação

### Pré-requisitos

Java 21+ · Docker · Maven (ou `./mvnw`)

### Subir ambiente

```bash
# Banco de dados (PostgreSQL)
docker-compose up -d db

# Aplicação (http://localhost:8080)
./mvnw spring-boot:run
```

PostgreSQL: host `localhost:5432` · banco `oficina_db` · usuário `oficina_user` · senha `oficina_123`

### Swagger

`http://localhost:8080/swagger-ui/index.html`

1. `POST /api/auth/register` → obter token
2. Clicar **Authorize** → colar o token (sem `Bearer`)

### Testes

```bash
./mvnw test                                          # Unitários + controller (H2)
./mvnw test -Dspring.profiles.active=integration     # Integração (PostgreSQL)
./mvnw verify                                        # Build completo + JaCoCo
```

Relatório de cobertura: `target/site/jacoco/index.html`

### Variáveis de Ambiente

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/oficina_db` |
| `DB_USER` | `oficina_user` |
| `DB_PASS` | `oficina_123` |
| `JWT_SECRET` | chave hardcoded de fallback |

### Insomnia

Importar `docs/collections/oficina-api.insomnia.json`. Variáveis: `base_url`, `token`,
`cliente_id`, `veiculo_placa`, `numero_os`, `numero_orcamento`.

**Fluxo mínimo de teste**:
`register → login → POST /clientes → POST /veiculos → POST /ordens-servico → avançar ciclo da OS`
