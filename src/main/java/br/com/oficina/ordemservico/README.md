# Domínio: OrdemDeServico

## Objetivo

Orquestrar todo o ciclo de vida de um serviço de manutenção veicular na oficina, desde a
abertura da OS até a entrega do veículo ao cliente. É o domínio central do sistema —
coordena clientes, veículos, diagnóstico, orçamento, execução e notificações.

## Responsabilidade Principal

Controlar a máquina de estados de uma ordem de serviço (OS), garantindo que as transições
sigam as regras de negócio da oficina, rastrear o tempo de execução e fornecer métricas
operacionais. Cada OS armazena um snapshot completo dos dados do funcionário, cliente e
veículo no momento de sua criação.

## Funcionalidades Implementadas

### Use Cases

| Use Case | Endpoint | Descrição |
|---|---|---|
| `CriarNovaOrdemDeServicoUseCase` | `POST /ordens-servico` | Abre nova OS vinculando funcionário, cliente e veículo com snapshot de dados |
| `AlterarOrdemDeServicoUseCase` | `PUT /ordens-servico/{numero}` | Atualiza observações e dados da OS |
| `ExcluirOrdemDeServicoUseCase` | `DELETE /ordens-servico/{numero}` | Remove a OS permanentemente |
| `ConsultarOrdensDeServicoUseCase` | `GET /ordens-servico` | Lista OS com filtros por número, status, situação, cliente e veículo |
| `AcompanharOrdemDeServicoUseCase` | `GET /ordens-servico/{numero}/acompanhamento` | Retorna situação simplificada para acompanhamento pelo cliente |
| `ConsultarStatusOrdemDeServicoUseCase` | `GET /ordens-servico/{numero}` | Retorna status técnico completo da OS |
| `IniciarDiagnosticoUseCase` | `POST /ordens-servico/{numero}/diagnostico/iniciar` | Transiciona OS_ABERTA → DIAGNOSTICO_EM_ANDAMENTO |
| `ConcluirDiagnosticoUseCase` | `POST /ordens-servico/{numero}/diagnostico/concluir` | Transiciona DIAGNOSTICO_EM_ANDAMENTO → DIAGNOSTICO_CONCLUIDO; corpo opcional grava **descrição do serviço + peças** (fonte do orçamento) |
| `EnviarDiagnosticoParaOrcamentoUseCase` | `POST /ordens-servico/{numero}/diagnostico/enviar-para-orcamento` | **Fluxo único** DIAGNOSTICO_CONCLUIDO → AGUARDANDO_APROVACAO; corpo só financeiro; deriva cliente/veículo/funcionário e **puxa peças do diagnóstico**; cria o Orçamento na mesma transação |
| `IniciarExecucaoUseCase` | `POST /ordens-servico/{numero}/execucao/iniciar` | Transiciona AGUARDANDO_APROVACAO → SERVICO_EM_ANDAMENTO |
| `ConcluirServicoUseCase` | `POST /ordens-servico/{numero}/servico/concluir` | Transiciona SERVICO_EM_ANDAMENTO → OS_FINALIZADA; consome peças do estoque |
| `FinalizarOrdemDeServicoUseCase` | `POST /ordens-servico/{numero}/finalizacao` | Transiciona ORCAMENTO_GERADO → OS_FINALIZADA; consome peças |
| `EntregarAoClienteUseCase` | `POST /ordens-servico/{numero}/entrega` | Transiciona OS_FINALIZADA → ENTREGUE |
| `CalcularTempoMedioExecucaoUseCase` | `GET /ordens-servico/metricas/tempo-medio` | Retorna tempo médio de execução de OS finalizadas |

### Máquina de Estados

```
           OS_ABERTA
               |
               | iniciarDiagnostico()
               ▼
   DIAGNOSTICO_EM_ANDAMENTO
               |
               | concluirDiagnostico()
               ▼
     DIAGNOSTICO_CONCLUIDO
               |
     ┌─────────┴──────────┐
     |                    |
enviarParaAprovacao()  enviarParaOrcamento()
(→AGUARDANDO_APROVACAO) (→ORCAMENTO_GERADO)
     |                    |
     |             finalizar() ──────────────────────┐
     |                                               |
     | [Decisão webhook: APROVADO]                   |
     | aprovarOrcamento()                            |
     ▼                                               |
AGUARDANDO_APROVACAO                                 |
     |                                               |
     | iniciarExecucao()                             |
     ▼                                               |
SERVICO_EM_ANDAMENTO                                 |
     |                                               |
     | concluirServico()                             |
     ▼                                               |
OS_FINALIZADA ◄────────────────────────────────────┘
     |
     | recusarOrcamento() [Decisão webhook: REJEITADO]
     |   ou rejeicao via DecidirOrcamentoExternamente
     |
OS_FINALIZADA (MotivoEncerramento.ORCAMENTO_RECUSADO)
     |
     | entregarAoCliente()
     ▼
  ENTREGUE
```

> **Status órfãos removidos** (spec `005-ordemservico-achados-residuais`, CRI-003): os valores
> `AGUARDANDO_ORCAMENTO`, `ORCAMENTO_APROVADO`, `AGUARDANDO_PECA` e `SERVICO_CONCLUIDO` foram
> **removidos** do enum `StatusOrdemDeServico` por não possuírem transição alguma. Os dados
> existentes já haviam sido realinhados pela migração `V14`. O enum passa a conter apenas os
> 8 status alcançáveis.

### Situações (visão do cliente)

O enum `SituacaoOrdemDeServico` é a representação simplificada para o cliente, mapeando
múltiplos status técnicos para 6 situações legíveis:

| Situação | Status técnicos mapeados |
|---|---|
| `RECEBIDA` | `OS_ABERTA` |
| `DIAGNOSTICO` | `DIAGNOSTICO_EM_ANDAMENTO`, `DIAGNOSTICO_CONCLUIDO` |
| `AGUARDANDO_APROVACAO` | `ORCAMENTO_GERADO`, `AGUARDANDO_APROVACAO` |
| `EXECUCAO` | `SERVICO_EM_ANDAMENTO` |
| `FINALIZADA` | `OS_FINALIZADA` |
| `ENTREGUE` | `ENTREGUE` |

### Snapshot de Dados

Ao criar uma OS, os seguintes dados são capturados e imutáveis:

| Campo snapshot | Origem |
|---|---|
| `funcionarioNome`, `funcionarioRegistro` | `funcionarioId` → `FuncionarioRepository` |
| `clienteNome`, `clienteDocumento`, `clienteTipo` | `clienteId` → `ClienteRepository` |
| `veiculoPlaca`, `veiculoMarca`, `veiculoModelo`, `veiculoAno` | `veiculoId` → `VeiculoRepository` |

Alterações posteriores nos cadastros de funcionário, cliente ou veículo não afetam OS
já criadas.

### Regras de Negócio

1. **Transições guardadas** — cada método de transição na `OrdemDeServico` verifica o
   `status` atual e lança `RegraDeNegocioException` se a transição não for permitida.
2. **Motivo de encerramento obrigatório** — `finalizar()` e `recusarOrcamento()` registram
   `MotivoEncerramento` (`SERVICO_CONCLUIDO` ou `ORCAMENTO_RECUSADO`).
3. **Consumo de peças na finalização** — `FinalizarOrdemDeServicoService` itera
   `orcamento.getPecasOrcamento()` e chama `ConsumirPecaUseCase` para cada peça antes de
   chamar `ordemDeServico.finalizar()`.
4. **Tempo de execução** — calculado em `FinalizarOrdemDeServicoService` como
   `dataFinalizacao - dataAbertura`, formatado em horas ou dias dependendo da duração.
5. **Evento de mudança de status** — `StatusOrdemDeServicoAlterado` é publicado sempre que
   uma transição de status ocorre; o módulo `notificacao` escuta esse evento de forma assíncrona.

## Ciclo de Vida

```
POST /ordens-servico
      |
      ▼
  [OS_ABERTA]
      |
      | POST /{num}/diagnostico/iniciar
      ▼
[DIAGNOSTICO_EM_ANDAMENTO]
      |
      | POST /{num}/diagnostico/concluir
      ▼
  [DIAGNOSTICO_CONCLUIDO]
      |
      | POST /{num}/diagnostico/enviar-para-orcamento  (cria Orçamento + transição, atômico)
      ▼
  [AGUARDANDO_APROVACAO]
      |
      ├── webhook aprova → POST /{num}/execucao/iniciar → [SERVICO_EM_ANDAMENTO] → POST /{num}/servico/concluir → [OS_FINALIZADA]
      |
      └── webhook rejeita → [OS_FINALIZADA (ORCAMENTO_RECUSADO)]
      |
      ▼
  [OS_FINALIZADA]
      |
      | POST /{num}/entrega
      ▼
  [ENTREGUE]
```

## Dependências Internas

| Módulo | Direção | Detalhe |
|---|---|---|
| `cliente` | `ordemservico` consome `cliente` | `CriarNovaOrdemDeServicoService` busca dados do cliente para snapshot |
| `veiculo` | `ordemservico` consome `veiculo` | `CriarNovaOrdemDeServicoService` busca dados do veículo para snapshot |
| `orcamento` | `ordemservico` cria e lê `orcamento` | `EnviarDiagnosticoParaOrcamentoService` cria o orçamento; `FinalizarOrdemDeServicoService` o consulta |
| `pecainsumo` | `ordemservico` consome `pecainsumo` | `FinalizarOrdemDeServicoService` chama `ConsumirPecaUseCase` para cada peça do orçamento |
| `notificacao` | `ordemservico` publica evento consumido por `notificacao` | Publica `StatusOrdemDeServicoAlterado` em todas as transições de status |

## Dependências Externas

Nenhuma direta. A integração com sistema externo (decisão de orçamento via webhook) é
tratada pelo módulo `orcamento`.

## Pontos de Entrada REST

Todos os endpoints exigem autenticação JWT.

---

### `POST /ordens-servico` — Criar OS

**Request body** (`CriarOrdemDeServicoRequest`):

| Campo | Tipo | Descrição |
|---|---|---|
| `funcionarioId` | `UUID` | ID do funcionário responsável |
| `clienteId` | `UUID` | ID do cliente proprietário |
| `veiculoId` | `UUID` | ID do veículo a ser atendido |
| `observacoes` | `String` | Observações iniciais (opcional) |
| `servicosSolicitados` | `List<String>` | Lista de serviços solicitados |
| `pecasPrevistas` | `List<String>` | Lista de peças previstas (opcional) |

**Respostas**: `201 Created` com `Location: /ordens-servico/{numero}`.

---

### `GET /ordens-servico` — Listar OS

**Query params**: `numero`, `status` (`StatusOrdemDeServico`), `situacao`
(`SituacaoOrdemDeServico`), `clienteId` (UUID), `veiculoPlaca` (String). Todos opcionais.

**Respostas**: `200 OK` com lista de `OrdemDeServicoResumoResponse`.

---

### `GET /ordens-servico/{numero}` — Consultar OS por número

**Respostas**: `200 OK` com `OrdemDeServicoResponse` completo ou `404 Not Found`.

---

### `GET /ordens-servico/{numero}/acompanhamento` — Acompanhamento pelo cliente

**Respostas**: `200 OK` com `AcompanhamentoOrdemDeServicoResponse` (situação simplificada
+ observações) ou `404 Not Found`.

---

### `GET /ordens-servico/metricas/tempo-medio` — Tempo médio de execução

**Respostas**: `200 OK` com `TempoMedioExecucaoResponse` contendo média de duração das
OS finalizadas.

---

### `PUT /ordens-servico/{numero}` — Alterar OS

**Respostas**: `204 No Content` ou `404 Not Found`.

---

### `DELETE /ordens-servico/{numero}` — Excluir OS

**Respostas**: `204 No Content` ou `404 Not Found`.

---

### Endpoints de Transição de Status

| Endpoint | Transição |
|---|---|
| `POST /{numero}/diagnostico/iniciar` | `OS_ABERTA → DIAGNOSTICO_EM_ANDAMENTO` |
| `POST /{numero}/diagnostico/concluir` | `DIAGNOSTICO_EM_ANDAMENTO → DIAGNOSTICO_CONCLUIDO` |
| `POST /{numero}/diagnostico/enviar-para-orcamento` | `DIAGNOSTICO_CONCLUIDO → AGUARDANDO_APROVACAO` + cria Orçamento (atômico) |
| `POST /{numero}/execucao/iniciar` | `AGUARDANDO_APROVACAO → SERVICO_EM_ANDAMENTO` |
| `POST /{numero}/servico/concluir` | `SERVICO_EM_ANDAMENTO → OS_FINALIZADA` |
| `POST /{numero}/finalizacao` | `ORCAMENTO_GERADO → OS_FINALIZADA` |
| `POST /{numero}/entrega` | `OS_FINALIZADA → ENTREGUE` |

Todos os endpoints de transição retornam `200 OK` com `OrdemDeServicoResponse` atualizado,
ou `400 Bad Request` se a transição for inválida para o status atual.

## Modelos de Domínio

### `OrdemDeServico` — Aggregate Root

| Campo | Tipo | Descrição |
|---|---|---|
| `numero` | `String` (UUID) | Identificador único da OS |
| `status` | `StatusOrdemDeServico` | Status técnico atual (8 valores) |
| `situacao` | `SituacaoOrdemDeServico` | Visão simplificada para o cliente (6 valores) |
| `motivoEncerramento` | `MotivoEncerramento` | `SERVICO_CONCLUIDO` ou `ORCAMENTO_RECUSADO`; só presente em OS finalizadas |
| `funcionarioId` | `UUID` | Referência ao funcionário |
| `funcionarioNome` | `String` | Snapshot do nome |
| `funcionarioRegistro` | `String` | Snapshot do registro profissional |
| `clienteId` | `UUID` | Referência ao cliente |
| `clienteNome` | `String` | Snapshot do nome |
| `clienteDocumento` | `String` | Snapshot do CPF/CNPJ |
| `clienteTipo` | `TipoCliente` | Snapshot do tipo (PF/PJ) |
| `veiculoId` | `UUID` | Referência ao veículo |
| `veiculoPlaca` | `String` | Snapshot da placa |
| `veiculoMarca` | `String` | Snapshot da marca |
| `veiculoModelo` | `String` | Snapshot do modelo |
| `veiculoAno` | `Integer` | Snapshot do ano |
| `observacoes` | `String` | Observações do funcionário |
| `servicosSolicitados` | `List<String>` | Lista de serviços (ElementCollection) |
| `pecasPrevistas` | `List<String>` | Lista de peças previstas (ElementCollection) |
| `dataAbertura` | `LocalDateTime` | Timestamp de criação |
| `dataFinalizacao` | `LocalDateTime` | Timestamp de finalização (quando aplicável) |

### `StatusOrdemDeServico` — Enum (8 valores)

`OS_ABERTA` · `DIAGNOSTICO_EM_ANDAMENTO` · `DIAGNOSTICO_CONCLUIDO` ·
`ORCAMENTO_GERADO` · `AGUARDANDO_APROVACAO` · `SERVICO_EM_ANDAMENTO` ·
`OS_FINALIZADA` · `ENTREGUE`

> Todos os 8 valores são alcançáveis e possuem transição. Os quatro status órfãos
> (`AGUARDANDO_ORCAMENTO`, `ORCAMENTO_APROVADO`, `AGUARDANDO_PECA`, `SERVICO_CONCLUIDO`) foram
> removidos na spec `005-ordemservico-achados-residuais` (CRI-003).

### `SituacaoOrdemDeServico` — Enum (6 valores)

`RECEBIDA` · `DIAGNOSTICO` · `AGUARDANDO_APROVACAO` · `EXECUCAO` · `FINALIZADA` · `ENTREGUE`

### `StatusOrdemDeServicoAlterado` — Evento de Domínio

| Campo | Tipo | Descrição |
|---|---|---|
| `numeroOrdemServico` | `String` | Número da OS |
| `clienteId` | `UUID` | ID do cliente |
| `situacaoAnterior` | `SituacaoOrdemDeServico` | Situação antes da transição |
| `novaSituacao` | `SituacaoOrdemDeServico` | Nova situação após a transição |
| `ocorridoEm` | `LocalDateTime` | Timestamp da mudança |

## Arquivos Críticos

| Arquivo | Responsabilidade |
|---|---|
| `ordemservico/domain/model/OrdemDeServico.java` | Aggregate root; máquina de estados completa; snapshot |
| `ordemservico/domain/model/StatusOrdemDeServico.java` | Enum dos 12 status técnicos |
| `ordemservico/domain/model/SituacaoOrdemDeServico.java` | Enum das 6 situações de negócio |
| `ordemservico/domain/event/StatusOrdemDeServicoAlterado.java` | Evento de domínio publicado em cada transição |
| `ordemservico/application/service/CriarNovaOrdemDeServicoService.java` | Coleta snapshots; persiste a OS; publica evento |
| `ordemservico/application/service/EnviarDiagnosticoParaOrcamentoService.java` | Cria Orçamento; publica evento |
| `ordemservico/application/service/FinalizarOrdemDeServicoService.java` | Consome peças do estoque; finaliza OS; calcula tempo |
| `ordemservico/application/service/ConsultarOrdensDeServicoService.java` | Filtragem em memória; sem paginação |
| `ordemservico/infrastructure/web/OrdemDeServicoController.java` | Controller com 15+ endpoints |

## Observações

- **Fluxo único para `AGUARDANDO_APROVACAO`** — `EnviarDiagnosticoParaOrcamentoService` é o
  único caminho que leva a OS de `DIAGNOSTICO_CONCLUIDO` a `AGUARDANDO_APROVACAO`: ele chama
  `enviarParaAprovacao()` na OS **e** `CadastrarNovoOrcamento` (cria o orçamento) na mesma
  transação (`@Transactional`), garantindo que toda OS em `AGUARDANDO_APROVACAO` tenha um
  orçamento associado. O caminho legado `POST /{num}/orcamento/enviar-aprovacao`
  (`EnviarParaAprovacaoService`), que transicionava sem criar orçamento, foi **removido**
  (spec `012-fix-aprovacao-sem-orcamento`).
- **Sem logging em `ordemservico`** — a constituição lista como dívida técnica a ausência
  de logging SLF4J nos services de `ordemservico`. Nenhum service do módulo emite logs de
  entrada/saída de operação.
- **Filtragem em memória no `ConsultarOrdensDeServicoService`** — a consulta carrega todas
  as OS em memória e aplica os filtros via stream, sem JPA Specification nem paginação.

---

## Pontos de Atenção

### Achados de Integridade Crítica

> **Curadoria (spec `005-ordemservico-achados-residuais`)**: auditoria confirmou que CRI-002 e
> CRI-004 já estavam resolvidos no código; CRI-001 e CRI-003 foram corrigidos por essa feature.
> Todos os quatro achados bloqueantes estão **resolvidos**.

#### [CRI-001] Ausência de logging nos services do módulo ✅ RESOLVIDO

**Componente afetado**: `*Service.java` do pacote `ordemservico/application/service/`

**Descrição**: o módulo evoluiu para emitir logging SLF4J de entrada/conclusão em todos os
services. O único resíduo era `ConsultarStatusOrdemDeServicoService`, que não logava — corrigido
na spec `005`, alinhando-o ao padrão dos demais services (Princípio V).

**Estado**: resolvido — 100% dos services de `ordemservico` emitem logging estruturado de
entrada e conclusão.

---

#### [CRI-002] Filtragem de OS em memória ✅ RESOLVIDO

**Componente afetado**: `ConsultarOrdensDeServicoService` · `JpaOrdemDeServicoRepository`

**Descrição**: a filtragem foi movida para o banco — `JpaOrdemDeServicoRepository` usa
`JpaSpecificationExecutor` e a query dedicada `buscarAtivasPriorizadasPorFiltros`; a filtragem
por stream em memória não existe mais.

**Estado**: resolvido. A paginação (`Pageable`) permanece como melhoria de backlog (MEL-002),
não-bloqueante.

---

#### [CRI-003] Status técnicos sem transições implementadas ✅ RESOLVIDO

**Componente afetado**: `StatusOrdemDeServico` · `SituacaoOrdemDeServico.fromStatus`

**Descrição**: os status órfãos `AGUARDANDO_ORCAMENTO`, `ORCAMENTO_APROVADO`, `AGUARDANDO_PECA`
e `SERVICO_CONCLUIDO` não possuíam transição de entrada nem de saída.

**Correção aplicada (spec `005`)**: os quatro valores foram **removidos** do enum e do `switch`
de `SituacaoOrdemDeServico.fromStatus`. A migração `V14` (pré-existente) já havia realinhado os
dados existentes para fora desses valores; como `status` é `EnumType.STRING`, a remoção é
data-safe e não exigiu nova migração. O enum passou de 12 para 8 valores, todos alcançáveis.

**Estado**: resolvido.

---

#### [CRI-004] Validação de veículo pertencente ao cliente na criação da OS ✅ RESOLVIDO

**Componente afetado**: `CriarNovaOrdemDeServicoService`

**Descrição**: a criação da OS verifica que o veículo pertence ao cliente informado —
`if (!veiculo.getClienteId().equals(cliente.getId()))` lança
`RegraDeNegocioException("Veiculo informado nao pertence ao cliente selecionado.")`.

**Estado**: resolvido — não é mais possível vincular um veículo de outro cliente à OS.

---

### Achados de Melhoria

#### [MEL-001] Anotações JPA no aggregate root — dívida técnica arquitetural

**Componente afetado**: `OrdemDeServico.java`

**Descrição**: `OrdemDeServico` possui `@Entity`, `@ElementCollection` e dezenas de
`@Column` diretamente no aggregate root, violando o Princípio I da constituição.

**Sugestão de backlog**: extrair `OrdemDeServicoJpaEntity` com `fromDomain()` / `toDomain()`,
seguindo o padrão `PecaInsumo` / `PecaInsumoJpaEntity`.

---

#### [MEL-002] Sem paginação nas consultas de OS

**Componente afetado**: `GET /ordens-servico` · `ConsultarOrdensDeServicoService`

**Descrição**: o endpoint de listagem retorna todas as OS sem paginação — mesmo com filtros
aplicados, o conjunto de resultados pode ser grande para clientes com histórico extenso.

**Sugestão de backlog**: adicionar parâmetros de paginação (`page`, `size`, `sort`) ao
endpoint e implementar via `Pageable` no repositório.

---

#### [MEL-003] Tempo médio de execução calculado na aplicação, não no banco

**Componente afetado**: `CalcularTempoMedioExecucaoService`

**Descrição**: o cálculo de tempo médio carrega todas as OS finalizadas em memória e
calcula a média via stream. Com histórico grande, essa operação é custosa.

**Sugestão de backlog**: delegar o cálculo ao banco via query JPQL
(`AVG(EXTRACT(EPOCH FROM (data_finalizacao - data_abertura)))`).

---

#### [MEL-004] Ausência de restrição de deleção para OS em andamento

**Componente afetado**: `ExcluirOrdemDeServicoService`

**Descrição**: uma OS pode ser excluída independente do status — inclusive OS em
`SERVICO_EM_ANDAMENTO` ou com peças já consumidas do estoque. Não há rollback de estoque
ao excluir.

**Sugestão de backlog**: restringir exclusão a OS no status `OS_ABERTA` apenas (ou adicionar
lógica de rollback de peças reservadas ao excluir uma OS com orçamento ativo).
