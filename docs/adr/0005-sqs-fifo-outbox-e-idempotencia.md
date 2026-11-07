# ADR 0005 — Mensageria com Amazon SQS FIFO, outbox transacional e idempotência

- **Status:** Proposto
- **Data:** 2026-10-15
- **Contexto:** Tech Challenge SOAT 13 — Fase 4

## Contexto

A Saga do [ADR 0004](0004-saga-orquestrada.md) troca comandos e eventos entre OS,
Billing e Execution. O enunciado exige **mensageria assíncrona** (RabbitMQ, Kafka,
SQS ou similar). Três problemas precisam de resposta, qualquer que seja o broker:

1. **Atomicidade entre banco e broker.** Gravar o estado e publicar a mensagem não
   é atômico. Se o pod cair entre os dois, a Saga trava (estado gravado, mensagem
   perdida) ou recebe algo que não aconteceu (mensagem enviada, transação desfeita).
2. **Entrega pelo menos uma vez.** Qualquer broker gerenciado reentrega; o
   consumidor precisa tolerar duplicatas.
3. **Ordem por OS.** `PagamentoConfirmado` não pode ser processado antes de
   `PecasReservadas` da mesma OS.

O `oficina-app` já resolve o problema 1 para e-mail: o módulo `notificacao` grava
um outbox na tabela `notificacoes` e reprocessa falhas por agendamento. O AWS SDK
v2 já está no `pom.xml` por causa do SES.

## Decisão

**Amazon SQS FIFO + DLQ, acessado pelo AWS SDK v2 puro, com outbox transacional no
produtor e idempotência no consumidor.**

### Filas

Cada serviço é dono das filas que **consome** e as cria no próprio `infra/`.

| Fila | Produtor | Consumidor | Mensagens |
|---|---|---|---|
| `oficina-<env>-billing-commands.fifo` | OS | Billing | Comandos para o Billing |
| `oficina-<env>-execution-commands.fifo` | OS | Execution | Comandos para a Execution |
| `oficina-<env>-os-saga-replies.fifo` | Billing, Execution | OS | Todos os eventos de resposta |

Cada uma com `-dlq.fifo` correspondente. Configuração:

| Atributo | Valor | Motivo |
|---|---|---|
| `FifoQueue` | `true` | Ordem por grupo |
| `ContentBasedDeduplication` | `false` | A deduplicação usa o `messageId` explícito |
| `MessageGroupId` | `ordemServicoId` | Mensagens da mesma OS em ordem; OSs diferentes em paralelo |
| `MessageDeduplicationId` | `messageId` do envelope | Reenvio do outbox em até 5 min é descartado pelo próprio SQS |
| `VisibilityTimeout` | 60 s | Folga sobre o tempo de processamento |
| `ReceiveMessageWaitTimeSeconds` | 20 s | Long polling: menos requisições, menos custo |
| `maxReceiveCount` (redrive) | 5 | Depois de 5 falhas a mensagem vai para a DLQ (monitorada) |
| Retenção | 4 dias (fila) / 14 dias (DLQ) | DLQ precisa durar até alguém olhar |

Produtores descobrem a URL por `GetQueueUrl` a partir do nome (convenção em
[`../arquitetura/convencoes-de-nomes.md`](../arquitetura/convencoes-de-nomes.md)),
sem depender do state Terraform de outro serviço. Permissões por IRSA, mínimas:
`sqs:SendMessage`/`GetQueueUrl` nas filas que o serviço produz;
`sqs:ReceiveMessage`/`DeleteMessage`/`ChangeMessageVisibility`/`GetQueueAttributes`
nas que consome.

### Envelope

Todas as mensagens usam o mesmo envelope, definido em
[`../contratos/mensagens.md`](../contratos/mensagens.md) e validado por JSON Schema
(`docs/contratos/schemas/`):

```json
{
  "messageId": "0b8f6d0e-6a1c-4d7e-9a53-2f1e0c3b9a10",
  "type": "GerarOrcamento",
  "version": 1,
  "sagaId": "6f0e2b8a-1c3d-4e5f-8a9b-0c1d2e3f4a5b",
  "ordemServicoId": "3c9a1b2d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
  "correlationId": "Yx2kLh1MIAMEJbQ=",
  "causationId": null,
  "occurredAt": "2026-10-15T13:00:00Z",
  "source": "oficina-os-service",
  "payload": { }
}
```

`type` e `version` também vão como *message attributes* do SQS, para o consumidor
rotear sem desserializar o corpo. `causationId` é o `messageId` do comando que
originou um evento de resposta. `correlationId` é o `x-request-id` gerado pelo API
Gateway, carregado de ponta a ponta.

### Outbox (produtor)

```sql
CREATE TABLE outbox_mensagens (
    id            uuid PRIMARY KEY,          -- = messageId
    fila_destino  varchar(120) NOT NULL,
    tipo          varchar(60)  NOT NULL,
    group_id      varchar(60)  NOT NULL,     -- = ordemServicoId
    payload       jsonb        NOT NULL,     -- envelope completo
    criada_em     timestamp    NOT NULL,
    publicada_em  timestamp,
    tentativas    int          NOT NULL DEFAULT 0,
    ultimo_erro   text
);
CREATE INDEX ix_outbox_pendentes ON outbox_mensagens (criada_em) WHERE publicada_em IS NULL;
```

1. Na **mesma transação** que muda o estado, a mensagem é gravada em `outbox_mensagens`.
2. Um publicador agendado (a cada 1 s) lê as pendentes em ordem de `criada_em`, envia e marca `publicada_em`.
3. Falhou o envio → incrementa `tentativas`, grava `ultimo_erro` e **para de enviar as mensagens seguintes do mesmo grupo** neste ciclo, para não inverter a ordem.
4. Com várias réplicas, só uma publica por vez: o ciclo começa com `pg_try_advisory_xact_lock`. O volume do projeto não justifica paralelismo no publicador; duplicata residual (pod cai depois de enviar e antes de marcar) é absorvida pela deduplicação do SQS e pela idempotência do consumidor.

Na Execution (DynamoDB) o item de outbox entra no mesmo `TransactWriteItems` da
mudança de estado, e o publicador lê os pendentes por um índice esparso
([ADR 0006](0006-dynamodb-no-execution-service.md)).

### Idempotência (consumidor)

```sql
CREATE TABLE mensagens_processadas (
    mensagem_id   uuid PRIMARY KEY,
    tipo          varchar(60) NOT NULL,
    processada_em timestamp   NOT NULL
);
```

- O consumidor grava o `messageId` em `mensagens_processadas` **na mesma transação do efeito**. Se já existe, apaga a mensagem da fila e não faz nada.
- Além disso, cada handler confere o **estado de negócio**: só reserva se a OS ainda não tem reserva, só estorna se o pagamento está `APROVADO` etc.
- **Comando reenviado por timeout tem `messageId` novo.** O participante percebe pelo estado que o efeito já foi aplicado e **republica a resposta** a partir do estado atual — é isso que destrava a Saga quando a resposta original se perdeu.
- No DynamoDB, a idempotência é um `Put` condicional (`attribute_not_exists(pk)`) dentro da transação, com TTL de 7 dias.

### Consumo e falhas

- Long polling de 20 s, até 10 mensagens; mensagens do mesmo grupo processadas em sequência.
- Sucesso → `DeleteMessage`. Exceção → a mensagem não é apagada e volta após o `VisibilityTimeout`; no FIFO isso **bloqueia o grupo** (a OS) até dar certo, preservando a ordem.
- 5 falhas → DLQ → monitor no Datadog. Mensagem que não passa no JSON Schema ou tem `type` desconhecido vai direto para a DLQ (não adianta tentar de novo).
- Evolução de contrato: campo novo opcional mantém `version`; mudança incompatível incrementa `version` e o consumidor aceita as duas durante a transição.

### Implementação

- **AWS SDK v2 puro** (`SqsClient`), sem Spring Cloud AWS — a dependência já existe e evita mais uma biblioteca a validar contra Spring Boot 4.
- Porta `MensageriaPort` no OS: adapter em memória no Passo 1, adapter SQS a partir do Passo 3. O orquestrador não sabe qual está ativo.
- Serialização com o Jackson 3 do Spring Boot 4 (`tools.jackson`): `Instant` e `UUID` saem como texto ISO sem configuração extra (verificado no spike).
- Local e testes de integração: LocalStack em Testcontainers e no `docker-compose`, com imagem fixada (ver spike, abaixo).

### Evidência do spike (Passo 0)

Spike descartável com Spring Boot 4.0.5, AWS SDK v2 2.46.6 e Testcontainers 1.21.4
contra `localstack/localstack:4.14.0`:

- fila FIFO com redrive para DLQ (`maxReceiveCount = 5`) criada pelo SDK;
- 4 envios no mesmo `MessageGroupId`, um deles com `MessageDeduplicationId` repetido → **3 recebidos, na ordem de envio**;
- `type` como *message attribute* lido no consumidor sem abrir o corpo.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| **RabbitMQ no cluster** | Mais um componente stateful para operar no EKS (volume, quotas, upgrade, monitoramento), competindo por CPU e memória com os três serviços num cluster já apertado. |
| **Amazon MQ (RabbitMQ gerenciado)** | Resolve a operação, mas custa por hora mesmo ocioso — desproporcional para infraestrutura que sobe e desce sob demanda. |
| **Kafka / Amazon MSK** | Ferramenta de streaming para volume alto e replay; o caso aqui é fila de comandos com poucas mensagens por OS. MSK é caro e Kafka no cluster é ainda mais pesado que RabbitMQ. |
| **SQS Standard** | Mais barato e sem limite de vazão, mas sem ordem: o orquestrador teria de reordenar eventos por conta própria. |
| **SNS + SQS (fan-out)** | Útil quando um evento tem vários consumidores; na orquestração cada mensagem tem exatamente um. Fica como evolução (ex.: read model de clientes para a Lambda). |
| **Spring Cloud AWS (`@SqsListener`)** | Menos código no consumidor, mas mais uma dependência a validar contra Spring Boot 4 e menos controle sobre ordem, visibilidade e DLQ. O consumidor próprio tem poucas dezenas de linhas. |
| **Publicar direto, sem outbox** | Reintroduz o problema 1. O padrão de outbox já existe no projeto (`notificacao`). |

## Consequências

**Positivas**

- Nenhum broker para operar; filas criadas pelo Terraform de cada serviço, com IAM por IRSA.
- Ordem por OS, deduplicação e DLQ nativas.
- Perda de pod não perde mensagem: ou está no outbox, ou na fila.
- Custo desprezível no volume do projeto (primeiro milhão de requisições SQS do mês é gratuito).

**Negativas e mitigações**

- **Acoplamento à AWS.** Isolado atrás de `MensageriaPort`; trocar de broker é trocar o adapter.
- **Latência de até ~1 s do outbox** (ciclo do publicador). Aceitável: as esperas da Saga são de minutos.
- **Mensagem com erro bloqueia a OS** até ir para a DLQ (5 tentativas × 60 s). Aceito: é o preço da ordem; o monitor da DLQ avisa.
- **LocalStack** passou a versionar por calendário e a exigir token nas imagens novas (ver spike); fixar a imagem e registrar a versão no README.

## Relacionado

- [ADR 0004](0004-saga-orquestrada.md) — Saga orquestrada
- [ADR 0006](0006-dynamodb-no-execution-service.md) — outbox e idempotência no DynamoDB
- [`../contratos/mensagens.md`](../contratos/mensagens.md) — contratos
- [`../spikes/0001-compatibilidade-fase4.md`](../spikes/0001-compatibilidade-fase4.md) — resultado do spike
