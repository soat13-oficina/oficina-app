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
| `CriarNovaOrdemDeServicoUseCase` | `POST /ordens-servico` | Abre nova OS vinculando funcionário, cliente e veículo com snapshot de dados; inicia a Saga |
| `AlterarOrdemDeServicoUseCase` | `PUT /ordens-servico/{numero}` | Atualiza observações e dados da OS |
| `ExcluirOrdemDeServicoUseCase` | `DELETE /ordens-servico/{numero}` | Remove a OS permanentemente |
| `ConsultarOrdensDeServicoUseCase` | `GET /ordens-servico` | Lista OS com filtros por número, status, situação, cliente e veículo (a fila não traz finalizadas, entregues nem canceladas) |
| `AcompanharOrdemDeServicoUseCase` | `GET /ordens-servico/{numero}/acompanhamento` | Retorna situação simplificada para acompanhamento pelo cliente |
| `ConsultarStatusOrdemDeServicoUseCase` | `GET /ordens-servico/{numero}` | Retorna status técnico completo da OS |
| `ConsultarHistoricoOrdemDeServicoUseCase` | `GET /ordens-servico/{numero}/historico` | Retorna a situação atual e **todas as mudanças de status**, com origem (`API`, `SAGA`, `TIMEOUT`, `MIGRACAO`) e saga — Fase 4 |
| `IniciarDiagnosticoUseCase` | `POST /ordens-servico/{numero}/diagnostico/iniciar` | Transiciona OS_ABERTA → DIAGNOSTICO_EM_ANDAMENTO |
| `ConcluirDiagnosticoUseCase` | `POST /ordens-servico/{numero}/diagnostico/concluir` | Transiciona DIAGNOSTICO_EM_ANDAMENTO → DIAGNOSTICO_CONCLUIDO; corpo opcional grava **descrição do serviço + peças** (fonte do orçamento); as peças são validadas no cadastro pela porta `CatalogoPecas` |
| `EnviarDiagnosticoParaOrcamentoUseCase` | `POST /ordens-servico/{numero}/diagnostico/enviar-para-orcamento` | **202 Accepted** — valida o que dá sem o Billing (diagnóstico concluído, mão de obra, desconto e validade; erro = 400) e pede o orçamento à Saga (`GerarOrcamento`). A OS só vai para AGUARDANDO_APROVACAO quando chega `OrcamentoGerado` — Fase 4 |
| `IniciarExecucaoUseCase` | `POST /ordens-servico/{numero}/execucao/iniciar` | Transiciona AGUARDANDO_EXECUCAO → SERVICO_EM_ANDAMENTO (a OS chega a AGUARDANDO_EXECUCAO pela Saga, quando o pagamento é confirmado) |
| `ConcluirServicoUseCase` | `POST /ordens-servico/{numero}/servico/concluir` | Transiciona SERVICO_EM_ANDAMENTO → OS_FINALIZADA e **baixa do estoque as peças reservadas** para a OS (porta `ReservasDaOrdem`), na mesma transação |
| `AbortarExecucaoUseCase` | `POST /ordens-servico/{numero}/execucao/abortar` | Mecânico aborta (corpo `{ "motivo": "..." }`): SERVICO_EM_ANDAMENTO → CANCELADA (`FALHA_EXECUCAO`); a Saga estorna o pagamento e libera as peças — Fase 4 |
| `EntregarAoClienteUseCase` | `POST /ordens-servico/{numero}/entrega` | Transiciona OS_FINALIZADA → ENTREGUE |
| `CalcularTempoMedioExecucaoUseCase` | `GET /ordens-servico/metricas/tempo-medio` | Retorna tempo médio de execução de OS finalizadas |
| `ConsultarSagaUseCase` | `GET /sagas/{ordemServicoId}` | Estado, prazo, link de pagamento, compensações pendentes e a história da Saga da OS — Fase 4 |

> **Removido na Fase 4:** `POST /ordens-servico/{numero}/finalizacao` (`FinalizarOrdemDeServicoUseCase`).
> Dependia do status `ORCAMENTO_GERADO`, que nenhum fluxo produzia — o endpoint nunca respondia
> sucesso. A finalização real sempre foi `POST /{numero}/servico/concluir`.

### Máquina de Estados

Setas com `[Saga]` são movidas pelo orquestrador a partir de eventos (origem `SAGA`) ou de prazos
vencidos (origem `TIMEOUT`); as demais são ações HTTP.

```
OS_ABERTA
   │ iniciarDiagnostico()
   ▼
DIAGNOSTICO_EM_ANDAMENTO ◄───────────────────────────┐
   │ concluirDiagnostico()                            │ [Saga] reabrirDiagnostico()
   ▼                                                  │ (OrcamentoFalhou com peça inválida)
DIAGNOSTICO_CONCLUIDO ────────────────────────────────┘
   │ [Saga] enviarParaAprovacao()      (OrcamentoGerado)
   ▼
AGUARDANDO_APROVACAO ──[Saga] recusarOrcamento() / expirarOrcamento()──► OS_FINALIZADA
   │ [Saga] aguardarPagamento()        (OrcamentoAprovado + PecasReservadas)   (ORCAMENTO_RECUSADO / _EXPIRADO)
   ▼
AGUARDANDO_PAGAMENTO ──[Saga] cancelarPorPagamento()──► CANCELADA (PAGAMENTO_RECUSADO / _EXPIRADO)
   │ [Saga] confirmarPagamento()       (PagamentoConfirmado)
   ▼
AGUARDANDO_EXECUCAO
   │ iniciarExecucao()
   ▼
SERVICO_EM_ANDAMENTO ──abortarExecucao()──► CANCELADA (FALHA_EXECUCAO)
   │ concluirServico()
   ▼
OS_FINALIZADA (SERVICO_CONCLUIDO)
   │ entregarAoCliente()
   ▼
ENTREGUE
```

Estoque insuficiente na reserva não move a OS: ela continua em AGUARDANDO_APROVACAO e o cliente
pode decidir de novo (a aprovação é revertida no orçamento). `ORCAMENTO_GERADO` continua no enum
só por dados antigos — nenhum fluxo o produz.

### Situações (visão do cliente)

O enum `SituacaoOrdemDeServico` é a representação simplificada para o cliente:

| Situação | Status técnicos mapeados |
|---|---|
| `RECEBIDA` | `OS_ABERTA` |
| `DIAGNOSTICO` | `DIAGNOSTICO_EM_ANDAMENTO`, `DIAGNOSTICO_CONCLUIDO` |
| `AGUARDANDO_APROVACAO` | `ORCAMENTO_GERADO`, `AGUARDANDO_APROVACAO` |
| `AGUARDANDO_PAGAMENTO` | `AGUARDANDO_PAGAMENTO` — Fase 4 |
| `EXECUCAO` | `AGUARDANDO_EXECUCAO`, `SERVICO_EM_ANDAMENTO` |
| `FINALIZADA` | `OS_FINALIZADA` |
| `ENTREGUE` | `ENTREGUE` |
| `CANCELADA` | `CANCELADA` — Fase 4 |

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
2. **Motivo de encerramento obrigatório** — toda OS finalizada ou cancelada registra
   `MotivoEncerramento`: `SERVICO_CONCLUIDO`, `ORCAMENTO_RECUSADO`, `ORCAMENTO_EXPIRADO`,
   `PAGAMENTO_RECUSADO`, `PAGAMENTO_EXPIRADO` ou `FALHA_EXECUCAO`.
3. **Diagnóstico e execução são do mecânico, na Execution** — o OS não tem mais endpoint para
   iniciar/concluir o diagnóstico, enviar para orçamento, iniciar/concluir/abortar a execução. O
   mecânico age em `POST /execucoes/...` (oficina-execution-service) e a Saga move a OS ao receber os
   eventos (`DiagnosticoIniciado`, `DiagnosticoConcluido`, `ExecucaoIniciada`, `ExecucaoConcluida`,
   `ExecucaoFalhou`). O diagnóstico concluído chega como **cópia** (serviços com mão de obra e peças,
   `OrdemDeServico.registrarDiagnostico`) e é a base do orçamento; a baixa do estoque na conclusão é da
   Execution (o evento `ExecucaoConcluida` já sai com as peças consumidas).
4. **Sem import entre módulos que viram serviços** — `ordemservico` não importa classes de
   `orcamento`, `pagamento` nem `pecainsumo` (regras ArchUnit em `ArquiteturaModulosTest`; esses
   módulos moram no Billing e na Execution). A única consulta síncrona é a das peças previstas na
   abertura: porta `CatalogoPecas` com adaptador REST (`infrastructure/catalogo`,
   `POST /internal/pecas-insumos/consulta` na Execution, `X-Service-Token`); o resto vai por mensagens.
5. **Evento de mudança de status** — `StatusOrdemDeServicoAlterado` é publicado sempre que
   uma transição de situação ocorre (pela API ou pela Saga); o módulo `notificacao` escuta esse
   evento de forma assíncrona.
6. **Histórico de status** (Fase 4, migration V21) — toda mudança de **status** (inclusive dentro
   da mesma situação) é acumulada pelo agregado em `alterarStatus` e gravada em
   `ordem_servico_historico` pelo repositório, no mesmo `salvar()` da ordem. A origem padrão é
   `API`; o orquestrador da Saga troca para `SAGA`/`TIMEOUT` com
   `registrarOrigemDasAlteracoes(origem, sagaId)` antes de mover a ordem.
7. **Saga orquestrada** (Fase 4, ADR 0004, migrations V23 e V24) — ver a seção "Saga" abaixo.

## Ciclo de Vida

```
POST /ordens-servico                                 → OS_ABERTA (Saga AGUARDANDO_DIAGNOSTICO; comando EnfileirarDiagnostico)
   DiagnosticoEnfileirado                            → (a OS espera na fila do mecânico)
DiagnosticoIniciado (Execution: mecânico)            → DIAGNOSTICO_EM_ANDAMENTO
DiagnosticoConcluido (Execution: mecânico)           → DIAGNOSTICO_CONCLUIDO → Saga GERANDO_ORCAMENTO (comando GerarOrcamento)
   OrcamentoGerado                                   → AGUARDANDO_APROVACAO
OrcamentoAprovado (Billing: cliente, funcionário ou integração) → Saga RESERVANDO_PECAS
   PecasReservadas                                   → AGUARDANDO_PAGAMENTO (link em GET /sagas/{ordemServicoId})
PagamentoConfirmado (Billing: webhook do Mercado Pago) → AGUARDANDO_EXECUCAO (comando EnfileirarExecucao)
ExecucaoIniciada (Execution: mecânico)               → SERVICO_EM_ANDAMENTO
ExecucaoConcluida (Execution: mecânico; estoque baixado lá) → OS_FINALIZADA
POST /{num}/entrega                                  → ENTREGUE
```

## Saga (Fase 4)

O orquestrador (`saga/application/OrquestradorSagaOrdemServico`) coordena o ciclo de vida da OS
com o Billing Service (orçamento e pagamento) e o Execution Service (fila, diagnóstico, execução e
estoque) **só por mensagens** — outbox `outbox_mensagens`, SQS (ADR 0005). Estado em
`saga_instancia`, história em `saga_passos`. A única entrada que não é mensagem é a abertura da OS
(`PassosDaSaga.iniciar`); o mecânico e o cliente chegam ao orquestrador como eventos.

| Momento | Quem dispara | Saga | OS |
|---|---|---|---|
| Abertura da OS | `POST /ordens-servico` | `AGUARDANDO_DIAGNOSTICO` → comando `EnfileirarDiagnostico` (vigia a resposta) | `OS_ABERTA` |
| Fila confirmada | evento `DiagnosticoEnfileirado` | segue `AGUARDANDO_DIAGNOSTICO`, sem prazo (depende do mecânico) | — |
| Mecânico inicia o diagnóstico | evento `DiagnosticoIniciado` | — | `DIAGNOSTICO_EM_ANDAMENTO` |
| Mecânico conclui o diagnóstico | evento `DiagnosticoConcluido` | `GERANDO_ORCAMENTO` → comando `GerarOrcamento` (sozinho) | `DIAGNOSTICO_CONCLUIDO` (copia serviços e peças) |
| Orçamento gerado | evento `OrcamentoGerado` | `AGUARDANDO_DECISAO`, prazo = `saga.prazo-decisao` ou a validade do orçamento | `AGUARDANDO_APROVACAO` |
| Orçamento não gerado | evento `OrcamentoFalhou` | `COMPENSANDO` → `ReabrirDiagnostico` → `DiagnosticoReaberto` → `AGUARDANDO_DIAGNOSTICO` (motivo guardado) | `DIAGNOSTICO_EM_ANDAMENTO` sem as peças inválidas; o mecânico refaz |
| Cliente aprova | evento `OrcamentoAprovado` | `RESERVANDO_PECAS` → comando `ReservarPecas` (tudo ou nada) | — |
| Peças reservadas | evento `PecasReservadas` | `AGUARDANDO_PAGAMENTO` → comando `SolicitarPagamento` (prazo `saga.prazo-pagamento`) | `AGUARDANDO_PAGAMENTO` |
| Estoque insuficiente | evento `ReservaFalhou` | `REVERTENDO_APROVACAO` → `ReverterAprovacao` → `AprovacaoRevertida` → `AGUARDANDO_DECISAO` com prazo novo | — |
| Cobrança criada | evento `PagamentoSolicitado` | guarda `pagamentoId` e `linkPagamento`; vigia o prazo + `saga.margem-pagamento` | — |
| Pagamento confirmado | evento `PagamentoConfirmado` | `AGUARDANDO_EXECUCAO` → comando `EnfileirarExecucao` (vigia a resposta) | `AGUARDANDO_EXECUCAO` |
| Fila de execução confirmada | evento `ExecucaoEnfileirada` | segue `AGUARDANDO_EXECUCAO`, sem prazo | — |
| Pagamento recusado / expirado | `PagamentoRecusado` / `PagamentoExpirado` (ou nada até o prazo + margem) | `COMPENSANDO` → `LiberarReserva` + `CancelarOrcamento` + `CancelarExecucao` → `COMPENSADA` | `CANCELADA` (`PAGAMENTO_RECUSADO` / `PAGAMENTO_EXPIRADO`) |
| Cliente rejeita | evento `OrcamentoRejeitado` | `COMPENSANDO` → `CancelarExecucao` → `ExecucaoCancelada` → `ENCERRADA_SEM_EXECUCAO` | `OS_FINALIZADA` / `ORCAMENTO_RECUSADO` |
| Prazo de decisão vence | `AgendadorPrazosSaga` (30 s) | `COMPENSANDO` → `CancelarOrcamento` + `CancelarExecucao` → `ENCERRADA_SEM_EXECUCAO` | `OS_FINALIZADA` / `ORCAMENTO_EXPIRADO` (origem `TIMEOUT`) |
| Mecânico inicia a execução | evento `ExecucaoIniciada` | `EM_EXECUCAO` | `SERVICO_EM_ANDAMENTO` |
| Mecânico conclui | evento `ExecucaoConcluida` (a Execution já baixou o estoque) | `CONCLUIDA` | `OS_FINALIZADA` / `SERVICO_CONCLUIDO` |
| Mecânico aborta | evento `ExecucaoFalhou` | `COMPENSANDO` → `EstornarPagamento` + `LiberarReserva` → `COMPENSADA` | `CANCELADA` (`FALHA_EXECUCAO`) |

**Timeouts.** Todo comando espera resposta por `saga.prazo-resposta`. Comando de avanço
(`EnfileirarDiagnostico`, `GerarOrcamento`, `ReservarPecas`, `SolicitarPagamento`,
`EnfileirarExecucao`) sem resposta é reenviado — mesmo payload, `messageId` novo, a partir do outbox
(`PublicadorMensagens.reenviar`) — até `saga.max-tentativas`; esgotou, o passo é tratado como falha
(orçamento: reabre o diagnóstico; reserva: libera o que houver e reverte a aprovação; cobrança:
cancela como pagamento expirado; fila da Execution sem confirmação: `FALHA_COMPENSACAO`, ação manual). Compensação sem
confirmação é reenviada até o mesmo limite e depois a Saga para em `FALHA_COMPENSACAO` (log de
erro, exige ação manual). `EstornoFalhou` mantém o estorno pendente e ele é pedido de novo no
próximo prazo.

Evento que não cabe no estado atual é registrado como `EVENTO_IGNORADO` e confirmado, nunca
reprocessado em laço. OS anterior à Saga ganha uma Saga "adotada" (estado derivado do status) no
primeiro evento — a V23 já cria uma para cada OS existente.

Configuração: `saga.prazo-decisao` (vazio em prd = validade do orçamento; `PT15M` em hml),
`saga.prazo-decisao-padrao` (`P7D`), `saga.prazo-resposta` (`PT5M`), `saga.max-tentativas` (3),
`saga.prazo-pagamento` (`PT24H`; `PT30M` em hml), `saga.margem-pagamento` (`PT10M`),
`saga.prazos.intervalo-ms` (30000).

BDD (Cucumber, roda no `./mvnw verify`): `src/test/resources/features/saga/` —
`fila-e-diagnostico.feature`, `decisao-do-orcamento.feature` e
`reserva-pagamento-e-compensacoes.feature`. A Execution e o Billing são os dublês `ExecutionSimulado`
e `BillingSimulado`, validados contra os JSON Schemas por `ContratoDasMensagensDaSagaTest`.

## Dependências Internas

| Módulo | Direção | Detalhe |
|---|---|---|
| `cliente` | `ordemservico` consome `cliente` | `CriarNovaOrdemDeServicoService` busca dados do cliente para snapshot |
| `veiculo` | `ordemservico` consome `veiculo` | `CriarNovaOrdemDeServicoService` busca dados do veículo para snapshot |
| `orcamento` / `pagamento` (Billing) | só mensagens | Envia `GerarOrcamento`, `ReverterAprovacao`, `CancelarOrcamento`, `SolicitarPagamento`, `EstornarPagamento`; consome os eventos correspondentes |
| `pecainsumo` / diagnóstico / execução (Execution) | mensagens + REST interno | Envia `EnfileirarDiagnostico`, `ReabrirDiagnostico`, `ReservarPecas`, `EnfileirarExecucao`, `LiberarReserva`, `CancelarExecucao`; consome os eventos correspondentes; `CatalogoPecas` (peças previstas na abertura) por `POST /internal/pecas-insumos/consulta` |
| `notificacao` | `ordemservico` publica evento consumido por `notificacao` | Publica `StatusOrdemDeServicoAlterado` em todas as transições de situação |

## Dependências Externas

Nenhuma direta. O provedor de pagamento (simulado no Passo 1, Mercado Pago no Passo 3) e a
decisão de orçamento por integração são tratados pelos módulos `pagamento` e `orcamento`.

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

### `GET /ordens-servico/{numero}/historico` — Histórico de status

**Respostas**: `200 OK` com `HistoricoOrdemDeServicoResponse` (`numeroOrdemServico`,
`situacaoAtual` e `alteracoes[]` com `statusAnterior`, `statusNovo`, `situacao`, `origem`,
`sagaId` e `ocorridoEm`, da mais antiga para a mais recente) ou `404 Not Found`.

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
| `POST /{numero}/diagnostico/enviar-para-orcamento` | **202**; a OS vai a `AGUARDANDO_APROVACAO` quando o Billing gera o orçamento (Saga) |
| `POST /{numero}/execucao/iniciar` | `AGUARDANDO_EXECUCAO → SERVICO_EM_ANDAMENTO` |
| `POST /{numero}/servico/concluir` | `SERVICO_EM_ANDAMENTO → OS_FINALIZADA` (baixa as peças reservadas) |
| `POST /{numero}/execucao/abortar` | `SERVICO_EM_ANDAMENTO → CANCELADA` (`FALHA_EXECUCAO`); corpo `{ "motivo": "..." }` obrigatório |
| `POST /{numero}/entrega` | `OS_FINALIZADA → ENTREGUE` |

Os endpoints de transição respondem `204 No Content` (o envio para orçamento, `202 Accepted`),
`400 Bad Request` se a transição for inválida para o status atual ou `404 Not Found`.

## Modelos de Domínio

### `OrdemDeServico` — Aggregate Root

| Campo | Tipo | Descrição |
|---|---|---|
| `numero` | `String` (UUID) | Identificador único da OS |
| `status` | `StatusOrdemDeServico` | Status técnico atual (11 valores) |
| `situacao` | `SituacaoOrdemDeServico` | Visão simplificada para o cliente (8 valores) |
| `motivoEncerramento` | `MotivoEncerramento` | Só presente em OS finalizadas ou canceladas (ver "Regras de Negócio") |
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

### `StatusOrdemDeServico` — Enum (11 valores)

`OS_ABERTA` · `DIAGNOSTICO_EM_ANDAMENTO` · `DIAGNOSTICO_CONCLUIDO` · `ORCAMENTO_GERADO` ·
`AGUARDANDO_APROVACAO` · `AGUARDANDO_PAGAMENTO` · `AGUARDANDO_EXECUCAO` ·
`SERVICO_EM_ANDAMENTO` · `OS_FINALIZADA` · `ENTREGUE` · `CANCELADA`

> Os quatro status órfãos (`AGUARDANDO_ORCAMENTO`, `ORCAMENTO_APROVADO`, `AGUARDANDO_PECA`,
> `SERVICO_CONCLUIDO`) foram removidos na spec `005-ordemservico-achados-residuais` (CRI-003).
>
> `ORCAMENTO_GERADO` não é produzido por nenhum fluxo (era o status exigido pela finalização
> legada, removida na Fase 4 junto com `enviarParaOrcamento()` e `finalizar()`). Fica no enum
> porque pode existir em dados antigos — a Saga adota essas OSs como aguardando decisão.

### `SituacaoOrdemDeServico` — Enum (8 valores)

`RECEBIDA` · `DIAGNOSTICO` · `AGUARDANDO_APROVACAO` · `AGUARDANDO_PAGAMENTO` · `EXECUCAO` ·
`FINALIZADA` · `ENTREGUE` · `CANCELADA`

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
| `ordemservico/domain/model/StatusOrdemDeServico.java` | Enum dos 11 status técnicos |
| `ordemservico/domain/model/SituacaoOrdemDeServico.java` | Enum das 8 situações de negócio |
| `ordemservico/domain/event/StatusOrdemDeServicoAlterado.java` | Evento de domínio publicado em cada transição |
| `ordemservico/application/service/CriarNovaOrdemDeServicoService.java` | Coleta snapshots; persiste a OS; inicia a Saga |
| `ordemservico/application/service/EnviarDiagnosticoParaOrcamentoService.java` | Valida dados financeiros e pede o orçamento à Saga |
| `ordemservico/application/service/ConcluirServicoService.java` | Conclui a OS e baixa as peças reservadas |
| `ordemservico/application/service/AbortarExecucaoService.java` | Cancela a OS e aciona as compensações da Saga |
| `ordemservico/application/port/` | Portas para o cadastro de peças e as reservas (adaptadores em `br.com.oficina.ponte`) |
| `ordemservico/saga/application/OrquestradorSagaOrdemServico.java` | Orquestrador: eventos, comandos, prazos e reenvios |
| `ordemservico/saga/domain/model/SagaOrdemServico.java` | Máquina de estados da Saga |
| `ordemservico/infrastructure/web/OrdemDeServicoController.java` | Controller com 15+ endpoints |

## Observações

- **Toda OS em `AGUARDANDO_APROVACAO` tem orçamento** — só o evento `OrcamentoGerado` leva a OS
  de `DIAGNOSTICO_CONCLUIDO` a `AGUARDANDO_APROVACAO`, e ele só sai do Billing depois de o
  orçamento ser gravado. O caminho legado `POST /{num}/orcamento/enviar-aprovacao`, que
  transicionava sem criar orçamento, foi **removido** (spec `012-fix-aprovacao-sem-orcamento`).
- **Toda OS em `AGUARDANDO_PAGAMENTO` ou adiante tem as peças reservadas** em `reservas_pecas`
  (a V24 regularizou as OSs aprovadas antes da Saga).

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
