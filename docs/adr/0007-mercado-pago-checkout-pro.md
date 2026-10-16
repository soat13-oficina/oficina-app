# ADR 0007 — Pagamento com Mercado Pago Checkout Pro, depois da aprovação e da reserva

- **Status:** Proposto
- **Data:** 2026-10-15
- **Contexto:** Tech Challenge SOAT 13 — Fase 4

## Contexto

O enunciado exige que os pagamentos sejam **integrados ao Mercado Pago**. Hoje não
existe nada de pagamento no projeto. Duas perguntas precisam de resposta:

1. **Em que momento da Saga o cliente paga?** Isso decide se existe uma compensação
   de pagamento (estorno) para demonstrar.
2. **Qual produto do Mercado Pago?** O MP oferece Checkout Pro (redireciona para uma
   página do MP), Checkout Transparente/Bricks (formulário no front-end da loja),
   Payments API direta (inclusive PIX) e a Orders API mais recente. O projeto não tem
   front-end, e a demonstração precisa forçar aprovação e recusa de forma previsível.

## Decisão

### Momento: depois da aprovação do orçamento e da reserva das peças, antes da execução

Passo 5 da Saga ([ADR 0004](0004-saga-orquestrada.md)). Pagar antes de executar
dá à Saga uma compensação real — **estorno no Mercado Pago** quando a execução
falha — e garante que a oficina não consome peças de um serviço não pago.

### Produto: Checkout Pro, em `binary_mode`

O Billing cria uma **preferência** e devolve o link (`init_point`; `sandbox_init_point`
em teste), enviado ao cliente por e-mail e exposto em `GET /orcamentos/{id}/pagamento`.
Com `binary_mode = true` o pagamento só termina **aprovado ou recusado** — sem
estados pendentes —, o que torna a Saga e a demonstração determinísticas.

### Fluxo

1. **`SolicitarPagamento`** chega ao Billing → cria `pagamentos` (`PENDENTE`, `idempotency_key` nova) e a preferência:

   | Campo | Valor |
   |---|---|
   | `items` | 1 item: "OS {numeroOrdemServico} — Orçamento {numeroOrcamento}", quantidade 1, `unit_price` = valor total, `BRL` |
   | `external_reference` | `pagamentoId` |
   | `notification_url` | `https://<api-gateway>/webhooks/mercadopago` |
   | `expires` / `expiration_date_to` | `true` / `expiraEm` recebido no comando (prazo definido pela Saga, `SAGA_PRAZO_PAGAMENTO`) |
   | `binary_mode` | `true` |
   | `payer.email` | e-mail do cliente (snapshot do orçamento) |

   Publica `PagamentoSolicitado` (link, expiração).
2. **Webhook** `POST /webhooks/mercadopago` (rota pública no API Gateway, sem authorizer):
   - valida `x-signature` com `WebhookSignatureValidator` do SDK — HMAC-SHA256 hexadecimal sobre o manifesto `id:{data.id};request-id:{x-request-id};ts:{ts};` (ts em segundos), tolerância de 5 min; inválido → `401`;
   - responde `200` imediatamente (o MP espera até 22 s e reenvia a cada 15 min);
   - **nunca confia no corpo**: consulta `GET /v1/payments/{data.id}` e localiza o pagamento por `external_reference`.
3. **Mapeamento de status do MP:**

   | Status no MP | `pagamentos.status` | Evento |
   |---|---|---|
   | `approved` | `APROVADO` (+ `mp_payment_id`) | `PagamentoConfirmado` |
   | `rejected`, `cancelled` | `RECUSADO` | `PagamentoRecusado` — e a preferência é expirada (`expiration_date_to = agora`) para impedir nova tentativa |
   | `pending`, `in_process` | sem mudança | — (não ocorre em `binary_mode`) |

4. **Expiração:** o Billing é a autoridade. Um agendamento marca `EXPIRADO` os pagamentos `PENDENTE` com `expira_em` vencido — **depois** de consultar o MP por `external_reference`, para não expirar um pagamento aprovado cujo webhook atrasou — e publica `PagamentoExpirado`.
5. **Aprovação tardia** (pagamento aprovado no MP para um registro já `RECUSADO`, `EXPIRADO` ou `CANCELADO`): o Billing **estorna automaticamente** e registra métrica/log. É uma regra local do Billing, não um passo da Saga.
6. **Compensações:**
   - `EstornarPagamento` → `POST /v1/payments/{id}/refunds` (estorno total) → `ESTORNADO`, orçamento `CANCELADO` → `PagamentoEstornado`. Erro de negócio do MP (4xx) → `EstornoFalhou` com o motivo; erro transitório (5xx, timeout) → exceção, a mensagem volta para a fila.
   - `CancelarOrcamento` com pagamento `PENDENTE` → expira a preferência e marca `CANCELADO`.
7. **Idempotência:** toda criação (preferência, estorno) leva `X-Idempotency-Key` persistida em `pagamentos` — o reenvio depois de uma queda reaproveita a mesma chave.

```sql
CREATE TABLE pagamentos (
    id                       uuid PRIMARY KEY,
    orcamento_id             uuid          NOT NULL REFERENCES orcamentos(id),
    ordem_de_servico_id      uuid          NOT NULL,   -- sem FK: dado de outro serviço
    saga_id                  uuid          NOT NULL,
    valor                    numeric(12,2) NOT NULL,
    status                   varchar(30)   NOT NULL,   -- PENDENTE, APROVADO, RECUSADO, EXPIRADO, ESTORNADO, CANCELADO
    mp_preference_id         varchar(80),
    mp_payment_id            varchar(40)   UNIQUE,
    link_pagamento           text,
    expira_em                timestamp     NOT NULL,
    idempotency_key          uuid          NOT NULL UNIQUE,
    estorno_idempotency_key  uuid,
    motivo                   varchar(200),
    criado_em                timestamp     NOT NULL,
    atualizado_em            timestamp     NOT NULL
);
```

### Implementação: SDK oficial `com.mercadopago:sdk-java` 3.x atrás de uma porta

- Porta `GatewayPagamentoPort` no Billing; adapter `MercadoPagoAdapter` com `PreferenceClient`, `PaymentClient`, `PaymentRefundClient` e `WebhookSignatureValidator`.
- O SDK aceita um `MPHttpClient` injetável. O adapter usa um **decorador** desse cliente por dois motivos:
  1. **Idempotência (achado do spike):** o SDK 3.7.0 sempre gera a própria `X-Idempotency-Key` e acrescenta a nossa em minúsculas — a requisição sai com **duas** chaves diferentes. O decorador remove a gerada quando a nossa existe.
  2. **Testes sem rede:** nos testes unitários o decorador delega a um dublê; nos de integração, a um cliente que reescreve a URL para o WireMock. **O CI nunca chama o MP real.**
- Timeouts curtos via `MPRequestOptions` (conexão 2 s, leitura 5 s); retries do SDK desligados — quem reprocessa é a fila.
- Credenciais no Secrets Manager `oficina/<env>/mercadopago` (`access_token`, `webhook_secret`), materializadas como Secret do Kubernetes pelo pipeline (mesmo padrão do `JWT_SECRET`).
- Desenvolvimento local: túnel (ngrok) para receber o webhook, ou `POST /webhooks/mercadopago/simulacao` habilitado **só** no perfil `local`.

### Ambiente de teste do Mercado Pago

- Conta de desenvolvedor e aplicação no painel (Checkout Pro, API de Preferences). Ao criar a aplicação, o MP cria automaticamente um **vendedor de teste**, cujas credenciais aparecem como **Credenciais de teste** da aplicação — o Access Token de teste começa com `APP_USR-`, como o de produção, então copie sempre da seção de teste. O **comprador de teste** é criado em *Contas de teste*, no mesmo país (Brasil).
- O comprador de teste paga o link em **janela anônima**; se o login pedir código por e-mail, ele aparece em *Suas integrações → aplicação → Contas de teste*.
- Webhook configurado no painel da aplicação com o tópico `payment`; o segredo exibido ali é o `webhook_secret`.
- Cartões de teste do Brasil com o **nome do titular** definindo o resultado: `APRO` (aprovado), `OTHE` (recusado por erro geral), `FUND` (saldo insuficiente); CPF de teste `12345678909`. Números dos cartões na [documentação de cartões de teste](https://www.mercadopago.com.br/developers/pt/docs/checkout-api/additional-content/your-integrations/test/cards).

### Evidência do spike (Passo 0)

SDK 3.7.0 com Spring Boot 4.0.5 e Java 21:

- `PreferenceClient.create` com `external_reference`, `notification_url`, expiração e `Authorization` por `MPRequestOptions`, sobre `MPHttpClient` dublê — resposta (`init_point`, `sandbox_init_point`) desserializada;
- `PaymentRefundClient.refund` chama `/v1/payments/{id}/refunds`;
- chave de idempotência duplicada reproduzida e corrigida pelo decorador;
- assinatura calculada pelo manifesto documentado é aceita pelo `WebhookSignatureValidator`; segredo errado, `data.id` trocado e `ts` fora da tolerância são recusados.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| **Pagamento na entrega** | Mais realista para oficina, mas a Saga perderia a compensação de pagamento e o estorno. |
| **Checkout Pro via Orders API** (`POST /v1/orders`, `checkout_url`) | É a recomendação do MP para integrações novas, e a Preferences API "continua com suporte, mas recursos novos só chegam na Orders API". Tem cancelamento e estorno próprios (`/v1/orders/{id}/cancel`, `/refund`) e o SDK 3.7.0 já traz `OrderClient`. Não foi escolhida porque o spike validou a Preferences, que cobre tudo o que a Saga precisa (expiração, `binary_mode`, estorno, webhook `payment`), e a Orders exigiria novo spike para expiração e tópico de notificação. A troca fica restrita ao `MercadoPagoAdapter`. |
| **Payments API com PIX** | Mais próximo do uso real no Brasil, mas aprovar um PIX em sandbox é menos previsível, e o QR Code exige um app de teste para "pagar" na demonstração. |
| **Checkout Transparente / Bricks** | Exige front-end para coletar o cartão; o projeto é só API. |
| **Checkout Pro sem `binary_mode`** | Permitiria estados pendentes (boleto, análise), que alongam a Saga sem acrescentar nada ao que se quer demonstrar. |
| **Chamadas REST com `RestClient`** em vez do SDK | Viável (são três endpoints), mas exigiria reescrever DTOs e a validação de assinatura que o SDK já oferece. O SDK passou no spike; o decorador resolve o único problema encontrado. Continua sendo o plano B se o SDK conflitar em versão futura. |
| **Recusa não encerra a cobrança** (cliente tenta outro cartão até expirar) | Mais amigável, mas a demonstração do cenário de recusa exigiria esperar o prazo. Fica como evolução configurável. |

## Consequências

**Positivas**

- Estorno real como compensação, demonstrável no vídeo com o cartão `OTHE` e com o aborto da execução.
- Resultado determinístico (`binary_mode`) e prazo de pagamento configurável para a demo.
- Webhook autenticado por assinatura e confirmado por consulta à API — o corpo da notificação nunca é fonte de verdade.

**Negativas e mitigações**

- **Webhook exige URL pública.** Rota no API Gateway sem authorizer, protegida pela assinatura; local com túnel ou simulador.
- **Dependências do SDK** (Apache HttpClient 4, Gson) entram só no Billing. Aceito; isoladas atrás da porta.
- **Recusa encerra a cobrança** na primeira tentativa. Simplificação consciente, documentada acima.
- **Credenciais de teste pessoais.** A conta de desenvolvedor pertence a um integrante; documentar no README quem é o dono e como gerar novas credenciais.

## Relacionado

- [ADR 0004](0004-saga-orquestrada.md) — passo 5 da Saga e compensações
- [ADR 0008](0008-exposicao-e-roteamento.md) — rota pública do webhook
- [`../contratos/mensagens.md`](../contratos/mensagens.md) — `SolicitarPagamento`, `PagamentoConfirmado`, `EstornarPagamento`…
- [`../spikes/0001-compatibilidade-fase4.md`](../spikes/0001-compatibilidade-fase4.md) — resultado do spike
- Documentação do Mercado Pago: [notificações webhook](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks)
