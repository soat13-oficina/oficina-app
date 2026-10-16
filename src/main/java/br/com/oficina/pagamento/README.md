# Domínio: Pagamento

## Objetivo

Cobrar o orçamento aprovado (passo 5 da Saga, ADR 0004) e devolver o dinheiro quando a execução é
abortada. Módulo novo da Fase 4 que vai, junto com `orcamento`, para o **Billing Service** no
Passo 3.

## Responsabilidade Principal

Criar a cobrança no provedor de pagamento, registrar o resultado (aprovado, recusado, expirado),
estornar e avisar a Saga de cada mudança. O Billing é a **autoridade do pagamento**: quem expira a
cobrança é ele; o OS Service só age por conta própria se nada chegar até o prazo + margem.

Nesta entrega o provedor é **simulado** (`GatewayPagamentoSimulado`): a cobrança ganha um link
fictício e o "pagamento" acontece por `POST /pagamentos/{id}/simulacao`, que faz o papel do cliente
pagando no checkout + webhook do provedor. No Passo 3 entra o Mercado Pago Checkout Pro (ADR 0007).

## Funcionalidades Implementadas

### Endpoints

| Endpoint | Classe | Descrição |
|---|---|---|
| `GET /pagamentos/{pagamentoId}` | `PagamentoController` | Valor, status, link, prazo, motivo e referências do provedor |
| `GET /orcamentos/{numeroOrcamento}/pagamento` | `PagamentoController` | Cobrança mais recente do orçamento — é onde o atendente pega o link para o cliente. 404 enquanto o orçamento não foi aprovado e as peças reservadas |
| `POST /pagamentos/{pagamentoId}/simulacao` | `SimulacaoPagamentoController` | Corpo `{ "resultado": "APROVADO" \| "RECUSADO", "motivo": "..." }`. Repetir o mesmo resultado não tem efeito; resultado diferente do já registrado (ou cobrança expirada/cancelada) responde 409; resultado ausente, 400. Desligável por `pagamento.simulacao.habilitada=false` |

Todos exigem JWT.

### Participante da Saga

Comandos recebidos em `billing-commands` (`infrastructure/mensageria/ManipuladorComandosPagamento`),
eventos em `os-saga-replies`. Contratos em `docs/contratos/mensagens.md`.

| Mensagem | Classe | O que acontece |
|---|---|---|
| comando `SolicitarPagamento` | `application/saga/PagamentosPelaSaga` | Cria a cobrança (`PENDENTE`) com o valor total do orçamento e o prazo do comando; responde `PagamentoSolicitado` com o link. Reenviado, encontra a cobrança vigente (pendente ou aprovada) e só repete a resposta |
| comando `EstornarPagamento` | `application/saga/PagamentosPelaSaga` | Execução abortada: estorna no provedor, marca `ESTORNADO`, cancela o orçamento e responde `PagamentoEstornado`. Já estornado: só repete a resposta. Provedor recusou, pagamento inexistente ou não aprovado: `EstornoFalhou` (a Saga tenta de novo até o limite) |
| evento `PagamentoConfirmado` / `PagamentoRecusado` | `application/ResultadoPagamentoService` | Resultado do pagamento (simulação hoje; webhook do Mercado Pago no Passo 3). Evento espontâneo, sem `causationId` |
| evento `PagamentoExpirado` | `application/ResultadoPagamentoService` + `AgendadorExpiracaoPagamentos` | A cada `pagamento.expiracao.intervalo-ms` (30 s), cobranças pendentes com prazo vencido expiram |

O cancelamento do orçamento pela Saga (`CancelarOrcamento`) também cancela a cobrança ainda
pendente (`CancelamentoDeCobrancas`, porta `PagamentosDoOrcamento` do `orcamento`), para um
pagamento tardio não ser aceito.

## Modelo de Domínio

### `Pagamento` (tabela `pagamentos`, V24)

| Campo | Descrição |
|---|---|
| `id`, `orcamentoId`, `ordemDeServicoId`, `sagaId` | Identidade e de onde veio (FK só para `orcamentos`, que mora no mesmo serviço) |
| `valor` | Valor total do orçamento no momento da cobrança |
| `status` | `PENDENTE`, `APROVADO`, `RECUSADO`, `EXPIRADO`, `ESTORNADO`, `CANCELADO` |
| `motivo` | Motivo da recusa, expiração, cancelamento ou estorno |
| `mpPreferenceId`, `mpPaymentId`, `mpRefundId` | Referências no provedor (preferência do Checkout Pro, pagamento, estorno). `mpPaymentId` é único |
| `linkPagamento` | Link que o cliente usa para pagar |
| `expiraEm` | Prazo para pagar (`saga.prazo-pagamento` a partir da reserva) |
| `idempotencyKey` | `X-Idempotency-Key` das chamadas ao Mercado Pago — a mesma em todas as tentativas desta cobrança |
| `versao` | Trava otimista (resultado e expiração podem chegar juntos) |

### Transições

```
PENDENTE ── aprovar() ──► APROVADO ── estornar() ──► ESTORNADO
   │
   ├── recusar() ──► RECUSADO
   ├── expirar() ──► EXPIRADO      (prazo vencido; só pendente expira)
   └── cancelar() ─► CANCELADO     (orçamento cancelado pela Saga)
```

Repetir a transição para o mesmo estado não é erro (webhook e comandos chegam mais de uma vez) e
não publica de novo; ir de um estado final para outro é `ConflitoDeRecursoException` (409).

## Dependências

| Módulo | Direção | Detalhe |
|---|---|---|
| `ordemservico` (Saga) | só mensagens | Consome `SolicitarPagamento` e `EstornarPagamento`; publica `PagamentoSolicitado`, `PagamentoConfirmado`, `PagamentoRecusado`, `PagamentoExpirado`, `PagamentoEstornado` e `EstornoFalhou` |
| `orcamento` | mesmo serviço (Billing) | Lê o orçamento (valor, número, placa) e o cancela no estorno; implementa a porta `PagamentosDoOrcamento` |
| Provedor de pagamento | porta `GatewayPagamento` | Simulado (`infrastructure/gateway/GatewayPagamentoSimulado`); Mercado Pago no Passo 3 |

## Configuração

| Propriedade | Padrão | Uso |
|---|---|---|
| `pagamento.simulacao.habilitada` | `true` | Liga o `POST /pagamentos/{id}/simulacao` |
| `pagamento.simulado.link-base` | `https://pagamento-simulado.oficina.local/checkout` | Base do link fictício |
| `pagamento.expiracao.agendamento-habilitado` | `true` | Desligado nos testes (chamam o serviço com o relógio que quiserem) |
| `pagamento.expiracao.intervalo-ms` | `30000` | Ciclo do expirador |
| `saga.prazo-pagamento` | `PT24H` (`PT30M` em hml) | Prazo dado ao cliente, vai no `SolicitarPagamento` |

## Arquivos Críticos

| Arquivo | Responsabilidade |
|---|---|
| `pagamento/domain/model/Pagamento.java` | Cobrança e suas transições |
| `pagamento/application/saga/PagamentosPelaSaga.java` | `SolicitarPagamento` e `EstornarPagamento` |
| `pagamento/application/saga/EventosPagamento.java` | Eventos para a Saga, gravados no outbox |
| `pagamento/application/ResultadoPagamentoService.java` | Resultado (simulação/webhook), expiração e consultas |
| `pagamento/application/CancelamentoDeCobrancas.java` | Cobrança pendente cancelada junto com o orçamento |
| `pagamento/application/port/GatewayPagamento.java` | Porta do provedor (Checkout Pro no Passo 3) |
| `pagamento/infrastructure/gateway/GatewayPagamentoSimulado.java` | Provedor simulado — sai no Passo 3 |

## Passo 3 (Billing Service + Mercado Pago)

Conforme o ADR 0007:

- `GatewayPagamento` real (`MercadoPagoAdapter`): preferência do Checkout Pro na criação, consulta
  do pagamento na API ao receber o webhook e estorno (`POST /v1/payments/{id}/refunds`), sempre com
  `idempotencyKey` (achado do spike: o SDK 3.7.0 duplica o header — decorar o `MPHttpClient`).
- Webhook público `POST /webhooks/mercadopago` (valida `x-signature`) no lugar da simulação; a
  simulação fica só no perfil `local`.
- A expiração consulta o Mercado Pago por `external_reference` antes de marcar `EXPIRADO`, para não
  expirar um pagamento aprovado cujo webhook atrasou.
- Credenciais no Secrets Manager `oficina/<env>/mercadopago` — nunca no repositório.
