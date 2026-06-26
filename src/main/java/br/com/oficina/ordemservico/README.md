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
| `ConcluirDiagnosticoUseCase` | `POST /ordens-servico/{numero}/diagnostico/concluir` | Transiciona DIAGNOSTICO_EM_ANDAMENTO → DIAGNOSTICO_CONCLUIDO |
| `EnviarDiagnosticoParaOrcamentoUseCase` | `POST /ordens-servico/{numero}/diagnostico/enviar-para-orcamento` | Transiciona DIAGNOSTICO_CONCLUIDO → AGUARDANDO_APROVACAO; cria Orçamento |
| `EnviarParaAprovacaoUseCase` | `POST /ordens-servico/{numero}/orcamento/enviar-aprovacao` | Transiciona AGUARDANDO_APROVACAO → ORCAMENTO_GERADO |
| `IniciarExecucaoUseCase` | `POST /ordens-servico/{numero}/execucao/iniciar` | Transiciona ORCAMENTO_APROVADO → SERVICO_EM_ANDAMENTO |
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
ORCAMENTO_APROVADO                                   |
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

> **Status sem implementação**: `AGUARDANDO_ORCAMENTO`, `ORCAMENTO_APROVADO`, `AGUARDANDO_PECA`
> e `SERVICO_CONCLUIDO` existem no enum `StatusOrdemDeServico` mas não possuem transições ou
> use cases correspondentes — são dívidas técnicas documentadas na constituição.

### Situações (visão do cliente)

O enum `SituacaoOrdemDeServico` é a representação simplificada para o cliente, mapeando
múltiplos status técnicos para 6 situações legíveis:

| Situação | Status técnicos mapeados |
|---|---|
| `RECEBIDA` | `OS_ABERTA` |
| `DIAGNOSTICO` | `DIAGNOSTICO_EM_ANDAMENTO`, `DIAGNOSTICO_CONCLUIDO` |
| `AGUARDANDO_APROVACAO` | `AGUARDANDO_ORCAMENTO`, `ORCAMENTO_GERADO`, `AGUARDANDO_APROVACAO`, `ORCAMENTO_APROVADO` |
| `EXECUCAO` | `SERVICO_EM_ANDAMENTO`, `AGUARDANDO_PECA` |
| `FINALIZADA` | `SERVICO_CONCLUIDO`, `OS_FINALIZADA` |
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
      ├── POST /{num}/diagnostico/enviar-para-orcamento → [AGUARDANDO_APROVACAO] → webhook aprova → [ORCAMENTO_APROVADO] → POST /{num}/execucao/iniciar → [SERVICO_EM_ANDAMENTO] → POST /{num}/servico/concluir → [OS_FINALIZADA]
      |
      └── POST /{num}/orcamento/enviar-aprovacao → [ORCAMENTO_GERADO] → POST /{num}/finalizacao → [OS_FINALIZADA]
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
| `POST /{numero}/diagnostico/enviar-para-orcamento` | `DIAGNOSTICO_CONCLUIDO → AGUARDANDO_APROVACAO` + cria Orçamento |
| `POST /{numero}/orcamento/enviar-aprovacao` | `AGUARDANDO_APROVACAO → ORCAMENTO_GERADO` |
| `POST /{numero}/execucao/iniciar` | `ORCAMENTO_APROVADO → SERVICO_EM_ANDAMENTO` |
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
| `status` | `StatusOrdemDeServico` | Status técnico atual (12 valores) |
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

### `StatusOrdemDeServico` — Enum (12 valores)

`OS_ABERTA` · `DIAGNOSTICO_EM_ANDAMENTO` · `DIAGNOSTICO_CONCLUIDO` ·
`AGUARDANDO_ORCAMENTO`* · `ORCAMENTO_GERADO` · `AGUARDANDO_APROVACAO` ·
`ORCAMENTO_APROVADO`* · `SERVICO_EM_ANDAMENTO` · `AGUARDANDO_PECA`* ·
`SERVICO_CONCLUIDO`* · `OS_FINALIZADA` · `ENTREGUE`

> (*) Sem transições implementadas atualmente.

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

- **`EnviarDiagnosticoParaOrcamentoService` chama `enviarParaAprovacao()` na OS** — a
  transição na OS para `AGUARDANDO_APROVACAO` ocorre neste service (não confundir com
  `EnviarParaAprovacaoService`, que transiciona para `ORCAMENTO_GERADO`). O nome pode ser
  confuso; o fluxo correto é: diagnóstico concluído → `enviarParaAprovacao()` (OS) +
  `CadastrarNovoOrcamento` (cria o orçamento) → aguardar decisão do webhook.
- **Sem logging em `ordemservico`** — a constituição lista como dívida técnica a ausência
  de logging SLF4J nos services de `ordemservico`. Nenhum service do módulo emite logs de
  entrada/saída de operação.
- **Filtragem em memória no `ConsultarOrdensDeServicoService`** — a consulta carrega todas
  as OS em memória e aplica os filtros via stream, sem JPA Specification nem paginação.

---

## Pontos de Atenção

### Achados de Integridade Crítica

#### [CRI-001] Ausência de logging em todos os services do módulo

**Componente afetado**: todos os `*Service.java` do pacote `ordemservico/application/service/`

**Descrição**: nenhum service de `ordemservico` emite logs SLF4J de entrada ou conclusão
de operações. A constituição (Princípio V) exige logging estruturado como requisito
não-negociável. Em produção, transições de status, criação de OS e consumo de peças são
invisíveis nos logs de aplicação.

**Impacto**: impossível rastrear a sequência de eventos em incidentes de produção; auditoria
de fluxo de OS indisponível.

**Correção sugerida**: adicionar `log.info("Recebida requisicao de ...")` e
`log.info("Operacao concluida...")` em todos os services, seguindo o padrão do
`VeiculoController`.

---

#### [CRI-002] Filtragem de OS em memória sem paginação

**Componente afetado**: `ConsultarOrdensDeServicoService`

**Descrição**: `buscarTodas()` carrega todas as OS do banco e filtra em memória via stream
pelos critérios de `ConsultarOrdensDeServicoQuery` (numero, status, situacao, clienteId,
veiculoPlaca). Em uma oficina com histórico significativo, essa operação degrada
linearmente e pode causar `OutOfMemoryError` com volume de dados.

**Impacto**: consulta de OS torna-se o gargalo principal de performance com crescimento do
banco; a resposta inclui todos os registros sem controle de volume.

**Correção sugerida**: implementar `JpaSpecificationExecutor` em `JpaOrdemDeServicoRepository`
com `Pageable`, substituindo a filtragem em memória por query dinâmica no banco — conforme
o padrão já existente em `JpaVeiculoRepository`.

---

#### [CRI-003] Status técnicos sem transições implementadas — lacuna de ciclo de vida

**Componente afetado**: `StatusOrdemDeServico` · máquina de estados em `OrdemDeServico`

**Descrição**: os status `AGUARDANDO_ORCAMENTO`, `ORCAMENTO_APROVADO`, `AGUARDANDO_PECA`
e `SERVICO_CONCLUIDO` existem no enum mas não possuem transições de entrada nem saída
implementadas. OS que por qualquer razão chegarem a esses status ficam presas sem possibilidade
de progressão.

**Impacto**: se uma OS atingir um status "órfão" (ex.: por inconsistência de dados), não
há operação disponível na API para progredi-la; a única saída é intervenção direta no banco.

**Correção sugerida**: ou implementar as transições faltantes, ou remover os status do enum
e garantir que nenhum dado existente os referencie via migração Flyway.

---

#### [CRI-004] Falta de validação de `veiculoId` pertencente ao `clienteId` na criação da OS

**Componente afetado**: `CriarNovaOrdemDeServicoService`

**Descrição**: ao criar uma OS, o service valida separadamente que o `clienteId` e o
`veiculoId` existem, mas não verifica se o veículo pertence ao cliente informado. É possível
criar uma OS vinculando um veículo de outro cliente.

**Impacto**: inconsistência de dados — a OS pode ter snapshot de cliente A com veículo de
cliente B; a OS de acompanhamento do cliente A exibirá um veículo que não é dele.

**Correção sugerida**: ao buscar o veículo, verificar que `veiculo.clienteId == clienteId`
da requisição, lançando `RegraDeNegocioException` se divergirem.

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
