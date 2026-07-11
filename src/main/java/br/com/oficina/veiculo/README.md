# Domínio: Veiculo

## Objetivo

Gerenciar o ciclo de vida de veículos cadastrados na oficina, sempre vinculados a um cliente
proprietário. Provê os contratos de busca por placa utilizados pelos domínios `ordemservico`
e `orcamento`.

## Responsabilidade Principal

Manter o cadastro de veículos como entidade independente, com identidade baseada em placa
normalizada e vínculo obrigatório com o cliente proprietário. O domínio garante unicidade de
placa, valida os formatos aceitos (padrão antigo e Mercosul) e normaliza a entrada antes de
qualquer operação de persistência ou consulta.

## Funcionalidades Implementadas

### Use Cases

| Use Case | Endpoint | Restrições |
|---|---|---|
| `CadastrarVeiculoUseCase` | `POST /veiculos` | Placa deve ser única; cliente proprietário deve existir; placa é normalizada e validada no aggregate |
| `AlterarVeiculoUseCase` | `PUT /veiculos/{placa}` | Veículo deve existir; placa é imutável — somente os demais atributos são atualizáveis |
| `ExcluirVeiculoUseCase` | `DELETE /veiculos/{placa}` | Veículo deve existir; remoção física permanente |
| `ConsultarVeiculosUseCase` | `GET /veiculos` | Todos os filtros são opcionais e combinados; retorna lista vazia se nenhum resultado |

### Métodos de Domínio em `Veiculo`

| Método | Descrição |
|---|---|
| `normalizarPlaca(String)` | Remove hífens e espaços, converte para maiúsculas e valida o formato via regex. Método estático invocado no construtor e no `JpaVeiculoRepository` antes de qualquer consulta. |
| `alterar(marca, modelo, fabricante, ano, potencia, cambio, tipo)` | Atualiza todos os atributos mutáveis. A `placa` não é parâmetro — é imutável após o cadastro. |
| `reconstituir(id, clienteId, ...)` | Factory method estático para recriar a entidade a partir da persistência, atribuindo o `id` diretamente sem passar pelo construtor público. |

### Regras de Negócio

1. **Unicidade de placa** — não é permitido cadastrar dois veículos com a mesma placa após normalização.
2. **Formato obrigatório** — a placa deve seguir o padrão antigo (`ABC1234` — 3 letras + 4 dígitos) ou Mercosul (`ABC1D23` — 3 letras + 1 dígito + 1 letra + 2 dígitos). Qualquer outro formato lança `RegraDeNegocioException("Placa do veiculo invalida")`.
3. **Normalização automática** — hífens e espaços são removidos e letras convertidas para maiúsculas antes de persistir, consultar ou comparar qualquer placa.
4. **Vínculo com cliente obrigatório no cadastro** — o `clienteId` informado deve referenciar um cliente existente (`RecursoNaoEncontradoException` se não encontrado).
5. **Placa imutável** — o endpoint de alteração identifica o veículo pela placa no path param; o payload de alteração não inclui `placa`, tornando-a estruturalmente imutável via API.

## Ciclo de Vida

O domínio `veiculo` não possui máquina de estados — seu ciclo de vida é linear:

```
[Inexistente]
      |
      | CadastrarVeiculo (POST /veiculos)
      ▼
 [Cadastrado] ◄──── AlterarVeiculo (PUT /veiculos/{placa})
      |
      | ExcluirVeiculo (DELETE /veiculos/{placa})
      ▼
 [Removido — permanente, sem soft delete]
```

Um veículo removido não pode ser recuperado. Não há histórico de versões nem auditoria de alterações.

## Dependências Internas

| Módulo | Direção | Detalhe |
|---|---|---|
| `cliente` | `veiculo` consome | `CadastrarVeiculoService` chama `ClienteRepository.buscarPorId()` para validar a existência do proprietário antes de persistir o veículo |
| `ordemservico` | `ordemservico` consome `veiculo` | `CriarNovaOrdemDeServicoService` chama `VeiculoRepository.buscarPorPlaca()` para obter os dados do veículo; valida também que o veículo pertence ao cliente informado na OS (`veiculo.getClienteId().equals(cliente.getId())`) |

> **Snapshot na OS**: ao criar uma `OrdemDeServico`, todos os atributos do veículo (placa, marca, modelo, fabricante, ano, potência, câmbio, tipo) são copiados como campos denormalizados na própria OS. O `veiculo_id` é mantido como FK de integridade referencial (`fk_ordens_de_servico_veiculo_id`), mas os dados do veículo dentro da OS são imutáveis independentemente de alterações futuras no cadastro do veículo.

## Dependências Externas

Nenhuma dependência externa (serviços de terceiros, mensageria, APIs externas).

## Pontos de Entrada REST

Todos os endpoints exigem autenticação JWT (Bearer token — `@SecurityRequirement(name = "bearerAuth")`).

---

### `POST /veiculos` — Cadastrar veículo

**Request body** (`CadastrarVeiculoRequest`):

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `placa` | `String` | Sim | Formato antigo ou Mercosul; normalizada antes de persistir |
| `marca` | `String` | Sim | Ex.: `"Toyota"` |
| `modelo` | `String` | Sim | Ex.: `"Corolla"` |
| `fabricante` | `String` | Sim | Ex.: `"Toyota Motor Corporation"` |
| `ano` | `int` | Sim | Ano do veículo. Ex.: `2024` |
| `potencia` | `int` | Sim | Potência em cavalos. Ex.: `177` |
| `cambio` | `String` | Sim | Texto livre. Ex.: `"AUTOMATICO"`, `"MANUAL"`, `"CVT"` |
| `tipo` | `TipoCombustivel` | Sim | Enum: `GASOLINA`, `FLEX`, `ELETRICO`, `DIESEL` |
| `clienteId` | `String` (UUID) | Sim | UUID do cliente proprietário |

**Respostas**:

| Status | Situação |
|---|---|
| 201 Created | Veículo criado com sucesso; `Location: /veiculos/{placa_normalizada}` |
| 400 Bad Request | Dados inválidos, placa com formato inválido ou placa já cadastrada |
| 404 Not Found | Cliente proprietário não encontrado para o `clienteId` informado |

---

### `PUT /veiculos/{placa}` — Alterar veículo

**Path param**: `placa` — aceita formato com ou sem hífen/espaço; normalizada antes da busca.

**Request body** (`AlterarVeiculoRequest`):

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `marca` | `String` | Sim | |
| `modelo` | `String` | Sim | |
| `fabricante` | `String` | Sim | |
| `ano` | `int` | Sim | |
| `potencia` | `int` | Sim | Em cavalos |
| `cambio` | `String` | Sim | |
| `tipo` | `TipoCombustivel` | Sim | |

**Respostas**:

| Status | Situação |
|---|---|
| 204 No Content | Veículo alterado com sucesso |
| 400 Bad Request | Dados inválidos ou placa com formato inválido |
| 404 Not Found | Veículo não encontrado para a placa informada |

---

### `GET /veiculos` — Consultar veículos

**Query params** (todos opcionais, combinados com `AND`):

| Param | Tipo | Descrição |
|---|---|---|
| `placa` | `String` | Filtra por placa exata (normalizada antes da comparação) |
| `marca` | `String` | Filtra por marca (exact match) |
| `fabricante` | `String` | Filtra por fabricante (exact match) |
| `ano` | `Integer` | Filtra por ano |
| `potencia` | `Integer` | Filtra por potência |
| `cambio` | `String` | Filtra por câmbio (exact match, case-sensitive) |
| `tipo` | `TipoCombustivel` | Filtra por tipo de combustível |

**Respostas**:

| Status | Situação |
|---|---|
| 200 OK | Lista de `VeiculoResponse`; vazia se nenhum veículo atender aos filtros |
| 400 Bad Request | Filtro de placa informado com formato inválido |

**Response body** (`VeiculoResponse`):

```json
{
  "clienteId": "11111111-1111-1111-1111-111111111111",
  "placa": "ABC1D23",
  "marca": "Toyota",
  "modelo": "Corolla",
  "fabricante": "Toyota Motor Corporation",
  "ano": 2024,
  "potencia": 177,
  "cambio": "AUTOMATICO",
  "tipo": "FLEX"
}
```

> `tipo` é serializado como `String` (nome do enum), não como objeto.

---

### `DELETE /veiculos/{placa}` — Excluir veículo

**Path param**: `placa` — normalizada antes da busca.

**Respostas**:

| Status | Situação |
|---|---|
| 204 No Content | Veículo excluído com sucesso |
| 400 Bad Request | Placa com formato inválido |
| 404 Not Found | Veículo não encontrado para a placa informada |

---

## Modelos de Domínio

### `Veiculo` — Aggregate Root

| Campo | Tipo Java | Coluna DB | Constraint | Descrição |
|---|---|---|---|---|
| `id` | `UUID` | `id` | PK, `NOT NULL`, gerado via `GenerationType.UUID` | Identificador interno do veículo |
| `clienteId` | `UUID` | `cliente_id` | `NOT NULL`, FK → `clientes.id` | Proprietário do veículo |
| `placa` | `String` | `placa` | `NOT NULL`, `UNIQUE` | Placa normalizada (sem hífen, maiúscula) |
| `marca` | `String` | `marca` | `NOT NULL` | Marca do veículo |
| `modelo` | `String` | `modelo` | `NOT NULL` | Modelo do veículo |
| `fabricante` | `String` | `fabricante` | `NOT NULL` | Fabricante do veículo |
| `ano` | `int` | `ano` | `NOT NULL` | Ano de fabricação |
| `potencia` | `int` | `potencia` | `NOT NULL` | Potência em cavalos |
| `cambio` | `String` | `cambio` | `NOT NULL` | Tipo de câmbio (texto livre) |
| `tipo` | `TipoCombustivel` | `tipo` | `NOT NULL`, `EnumType.STRING` | Tipo de combustível |

**Invariantes**:
- `placa` é validada e normalizada no construtor via `normalizarPlaca()` e é imutável — não há setter nem parâmetro de `placa` no método `alterar()`.
- `clienteId` é atribuído apenas no construtor ou via `reconstituir()` — não há setter público.
- O construtor protegido sem argumentos é reservado ao JPA e nunca deve ser usado diretamente por código de negócio.

### `TipoCombustivel` — Enum

| Valor | Descrição |
|---|---|
| `GASOLINA` | Motor a gasolina |
| `FLEX` | Motor flex (gasolina + etanol) |
| `ELETRICO` | Motor elétrico puro |
| `DIESEL` | Motor a diesel |

### Constraints de banco

| Constraint | Tipo | Definição |
|---|---|---|
| `pk_veiculos` | PRIMARY KEY | `veiculos.id` |
| `uk_veiculos_placa` | UNIQUE | `veiculos.placa` |
| `fk_veiculos_cliente_id` | FOREIGN KEY | `veiculos.cliente_id → clientes.id` (criada em `V8`) |
| `fk_ordens_de_servico_veiculo_id` | FOREIGN KEY | `ordens_de_servico.veiculo_id → veiculos.id` (criada em `V8`) |

## Arquivos Críticos

| Arquivo | Responsabilidade |
|---|---|
| `veiculo/domain/model/Veiculo.java` | Aggregate root; normalização e validação de placa; método `alterar()`; construtores e factory methods |
| `veiculo/domain/model/TipoCombustivel.java` | Enum dos tipos de combustível aceitos |
| `veiculo/domain/repository/VeiculoRepository.java` | Port (interface) de persistência — contrato do domínio |
| `veiculo/application/service/CadastrarVeiculoService.java` | Valida unicidade de placa e existência do cliente antes de persistir |
| `veiculo/application/service/AlterarVeiculoService.java` | Busca por placa e delega `alterar()` ao aggregate |
| `veiculo/application/service/ExcluirVeiculoService.java` | Verifica existência e executa remoção física — ver [CRI-001] |
| `veiculo/application/service/ConsultarVeiculosService.java` | Delega ao repositório com filtros combinados |
| `veiculo/infrastructure/persistence/JpaVeiculoRepository.java` | Adapter JPA; usa `Specification` para filtros dinâmicos |
| `veiculo/infrastructure/persistence/SpringDataVeiculoRepository.java` | Spring Data JPA com `findByPlaca` e `deleteByPlaca` |
| `db/migration/V8__alinhar_relacionamentos_e_pecas_orcamento.sql` | Cria FKs `fk_veiculos_cliente_id` e `fk_ordens_de_servico_veiculo_id` |

## Observações

- **Normalização como contrato duplo**: a normalização ocorre tanto no construtor de `Veiculo` (ao criar ou reconstituir) quanto no `JpaVeiculoRepository` antes de qualquer consulta. Isso garante consistência mas gera redundância controlada — código que usa o repositório diretamente não precisa normalizar a placa previamente.
- **Placa como identificador de negócio nas APIs**: a placa é o identificador público do veículo (path param e filtros). O UUID interno (`id`) não é exposto em nenhuma resposta nem usado como chave de acesso REST.
- **Filtros por igualdade exata**: o endpoint `GET /veiculos` usa `Specification` com `criteriaBuilder.equal()`. Todos os filtros são por igualdade exata — não há suporte a buscas parciais (LIKE) nem a comparação case-insensitive (exceto `placa`, que é sempre normalizada antes).
- **`clienteId` não revalidado na alteração**: `AlterarVeiculoService` não verifica se o cliente vinculado ao veículo ainda existe. Isso é intencional — a operação altera apenas os dados técnicos do veículo, sem alterar o proprietário.

---

## Pontos de Atenção

### Achados de Integridade Crítica

Os achados abaixo comprometem o comportamento funcional esperado do domínio e **devem ser implementados para garantir a corretude dos use cases em produção**.

---

#### [CRI-001] Hard delete sem guarda de integridade referencial

> ✅ **RESOLVIDO** na spec `003-veiculo-achados-criticos` — `ExcluirVeiculoService` agora verifica `OrdemDeServicoRepository.existePorVeiculoId(...)` e lança `RegraDeNegocioException` (HTTP 400) quando há OS vinculada (qualquer status).

**Componente afetado**: `ExcluirVeiculoService` · `JpaVeiculoRepository#excluirPorPlaca`

**Descrição**: `ExcluirVeiculoService` executa a remoção física do veículo sem verificar se existem ordens de serviço vinculadas. A migração `V8` criou a constraint `fk_ordens_de_servico_veiculo_id` (`ordens_de_servico.veiculo_id → veiculos.id`) no banco de dados. Ao tentar excluir um veículo que possui ao menos uma OS associada, o banco lança `DataIntegrityViolationException`. O `ApiExceptionHandler` não mapeia essa exceção — nenhum `@ExceptionHandler(DataIntegrityViolationException.class)` está registrado — resultando em HTTP 500 em vez de um erro de negócio controlado.

**Impacto funcional**: o endpoint `DELETE /veiculos/{placa}` retorna HTTP 500 em um cenário operacionalmente esperado para qualquer oficina em operação, pois veículos com histórico de OS são a regra, não a exceção. O erro 500 não comunica qual restrição foi violada, impossibilitando tratamento adequado por sistemas consumidores.

**Correção sugerida**: no `ExcluirVeiculoService`, verificar a existência de OS vinculadas antes da remoção (via `OrdemDeServicoRepository.existePorVeiculoId(veiculoId)` ou equivalente) e lançar `RegraDeNegocioException` com mensagem descritiva. Alternativamente, definir explicitamente a política de deleção (bloqueio enquanto houver OS ativa, soft delete, etc.) e aplicar a guarda correspondente.

---

#### [CRI-002] Ausência de validação de intervalo em `ano` e `potencia`

> ✅ **RESOLVIDO** na spec `003-veiculo-achados-criticos` — validação em duas camadas: Bean Validation (`@Min(1886)`/`@Positive`) nos DTOs e invariantes no aggregate `Veiculo` (`ano` em `[1886, ano corrente + 1]`, `potencia > 0`).

**Componente afetado**: `Veiculo` (construtor + `alterar()`) · `CadastrarVeiculoRequest` · `AlterarVeiculoRequest`

**Descrição**: os campos `ano` e `potencia` são `int` primitivos sem restrições de domínio nem de Bean Validation. Valores como `ano=0`, `ano=2999`, `potencia=0` ou `potencia=-100` são aceitos pelo aggregate, persistidos na tabela `veiculos` e propagados como snapshot denormalizado na criação de `OrdemDeServico`. O snapshot corrompido torna-se permanente no histórico de serviços.

**Impacto funcional**: dados semanticamente inválidos são silenciosamente aceitos e propagados para outros domínios sem nenhum log de alerta. A anomalia só é percebida por leitura direta do banco ou da OS, não no momento do cadastro.

**Correção sugerida**: adicionar `@Positive` em `potencia` e `@Min(1886)` / `@Max` em `ano` nos DTOs de request (Bean Validation); adicionar guarda equivalente no construtor e em `alterar()` do aggregate, lançando `RegraDeNegocioException` para valores fora do intervalo.

---

### Achados de Melhoria

Os achados abaixo não comprometem o funcionamento atual do domínio, mas representam dívidas técnicas ou lacunas de qualidade que **devem ser registradas no backlog**.

---

#### [MEL-001] Anotações JPA no aggregate root — dívida técnica arquitetural

**Componente afetado**: `Veiculo.java`

**Descrição**: `Veiculo` possui `@Entity`, `@Table`, `@Column`, `@Enumerated` e demais anotações JPA diretamente no aggregate root, acoplando o modelo de domínio ao framework de persistência. A constituição do projeto (Princípio I) registra isso explicitamente como dívida técnica dos módulos `veiculo`, `cliente`, `ordemservico` e `orcamento`, apontando `pecainsumo` como referência correta — `*JpaEntity` separada com `fromDomain()` / `toDomain()`.

**Impacto**: o domínio não pode ser testado sem o contexto JPA; evolução do schema impacta diretamente o aggregate; o construtor protegido sem argumentos existe exclusivamente para o JPA, poluindo a API do modelo de domínio.

**Sugestão de backlog**: criar `VeiculoJpaEntity` separada e remover as anotações JPA do aggregate `Veiculo`, seguindo o padrão de `pecainsumo`.

---

#### [MEL-002] Campo `cambio` sem tipo enumerado

**Componente afetado**: `Veiculo.java` · `CadastrarVeiculoRequest` · `AlterarVeiculoRequest`

**Descrição**: `cambio` é um `String` livre sem enum nem validação de formato. Valores como `"AUTOMATICO"`, `"Automatico"`, `"AUTO"` e `"CVT"` são semanticamente equivalentes mas distintos para o filtro do `GET /veiculos`, que usa comparação por igualdade exata (`criteriaBuilder.equal()`). Isso torna o filtro por câmbio não confiável e introduz inconsistência nos dados persistidos e nos snapshots de OS.

**Sugestão de backlog**: criar enum `TipoCambio` (ex.: `MANUAL`, `AUTOMATICO`, `CVT`, `AMT`) e migrar o campo com migração Flyway `V{N}__adicionar_enum_tipo_cambio.sql`.

---

#### [MEL-003] Ausência de paginação no endpoint de consulta

**Componente afetado**: `GET /veiculos` · `ConsultarVeiculosService`

**Descrição**: o endpoint retorna todos os registros que atendem aos filtros, sem limite ou paginação. Uma consulta sem parâmetros (`GET /veiculos`) carrega toda a tabela `veiculos` em memória. Esse padrão está identificado como dívida técnica global na constituição (Débito V).

**Sugestão de backlog**: adicionar suporte a `Pageable` (Spring Data) com parâmetros `page` e `size` na query string. Aplicar a mesma correção ao filtro de `ordemservico`, onde a dívida é análoga.

---

#### [MEL-004] Mensagem de erro genérica para valor inválido de `TipoCombustivel`

**Componente afetado**: `ApiExceptionHandler` · `CadastrarVeiculoRequest` · `AlterarVeiculoRequest`

**Descrição**: ao enviar um valor inválido para o campo `tipo` (ex.: `"HIBRIDO"`), o desserializador Jackson lança `HttpMessageNotReadableException`. O `ApiExceptionHandler` retorna `"Os dados enviados sao invalidos. Revise o corpo da requisicao."` sem indicar quais valores são aceitos. O tratamento específico de enum com listagem de valores válidos existe apenas para `CategoriaPeca`, criando inconsistência no contrato de erros da API.

**Sugestão de backlog**: estender o `handleInvalidRequestBody` para detectar falhas de desserialização de `TipoCombustivel` (e idealmente qualquer enum de domínio) e retornar a lista de valores aceitos, como já feito para `CategoriaPeca`.

---

#### [MEL-005] Ausência de endpoint e método de busca por ID interno

**Componente afetado**: `VeiculoController` · `VeiculoRepository`

**Descrição**: não existe endpoint `GET /veiculos/{id}` (por UUID) nem método `buscarPorId()` no `VeiculoRepository`. A única busca individual é por placa via filtro no `GET /veiculos`. Sistemas integrados que armazenam o `veiculoId` (presente nas OS) não têm caminho REST direto para recuperar os dados atuais do veículo pelo ID interno.

**Sugestão de backlog**: adicionar `buscarPorId(UUID id): Optional<Veiculo>` ao `VeiculoRepository` e o endpoint correspondente `GET /veiculos/{id}`.

---

#### [MEL-006] Remoção permanente sem reversibilidade

**Componente afetado**: `ExcluirVeiculoService` · `JpaVeiculoRepository#excluirPorPlaca`

**Descrição**: a exclusão é um hard delete irreversível. Não há registro de auditoria, data de remoção nem possibilidade de recuperação. O `veiculo_id` nas OS históricas fica como referência orfã (FK com CASCADE não configurado), o que é atualmente tolerado porque o snapshot da OS é autossuficiente.

**Sugestão de backlog**: avaliar soft delete com campo `removido_em TIMESTAMP NULL` na tabela `veiculos`, preservando o registro histórico e a integridade referencial com as OS existentes.
