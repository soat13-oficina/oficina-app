# Mensageria da Saga: SQS por fila e desenvolvimento local

Como o OS Service troca comandos e eventos com os participantes da Saga enquanto eles são
extraídos um a um ([ADR 0004](../adr/0004-saga-orquestrada.md), [ADR 0005](../adr/0005-sqs-fifo-outbox-e-idempotencia.md)).

## Um transporte por fila

Toda mensagem passa pelo outbox (`outbox_mensagens`) e é entregue pelo publicador agendado. O
`TransportePorFila` decide, **fila a fila**, como entregar:

| Fila lógica | Passos 1 e 2 | Passo 3 (Billing extraído) | Passo 4 (hoje: Execution extraída) |
|---|---|---|---|
| `billing-commands` | em memória | **SQS** | SQS |
| `execution-commands` | em memória | em memória | **SQS** |
| `os-saga-replies` | em memória | em memória (estoque) + **SQS** (Billing, lido pelo `ReceptorSqs`) | **SQS** (Billing e Execution) |

"Em memória" é entregar direto ao `ConsumidorMensagens`, no mesmo processo — o participante mora no
monólito. **Hoje nenhum participante mora no OS**: sem
`MENSAGERIA_SQS_ENVIO=billing-commands,execution-commands`, o `GerarOrcamento` e o
`EnfileirarDiagnostico` falham no outbox ("Nenhum manipulador para o tipo ...") e a Saga desiste do
passo depois dos reenvios. O caminho em memória segue existindo como costura de teste: os testes do OS
atendem as duas filas com o `BillingSimulado` e o `ExecutionSimulado`. O orquestrador não sabe qual
caminho cada mensagem fez: o envelope, a idempotência (`mensagens_processadas`) e o `source` de cada
evento são os mesmos.

Configuração (`application.yml`, prefixo `mensageria.sqs`):

| Propriedade | Variável | Padrão | Uso |
|---|---|---|---|
| `habilitado` | `MENSAGERIA_SQS_HABILITADO` | `false` | Cria o cliente SQS, o transporte e o receptor |
| `envio` | `MENSAGERIA_SQS_ENVIO` | vazio | Filas que saem pelo SQS (ex.: `billing-commands,execution-commands`) |
| `consumo` | `MENSAGERIA_SQS_CONSUMO` | vazio | Filas que o serviço lê do SQS (ex.: `os-saga-replies`) |
| `ambiente` | `AMBIENTE` | `local` | Nome físico `oficina-<ambiente>-<fila>.fifo` |
| `endpoint` | `AWS_ENDPOINT_URL_SQS` | vazio (AWS) | LocalStack |
| `regiao` | `AWS_REGION` | `us-east-1` | |

Credenciais pela cadeia padrão do AWS SDK v2: IRSA no EKS, `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`
no LocalStack (qualquer valor).

## Envio e recebimento

- **Envio** (`TransporteSqs`): `MessageGroupId` = `ordemServicoId`, `MessageDeduplicationId` =
  `messageId`, atributos `type`, `version` e `correlationId`. URL descoberta por `GetQueueUrl` a
  partir do nome. Falhou → o outbox conta a tentativa e reenvia no próximo ciclo.
- **Recebimento** (`ReceptorSqs`): uma thread por fila de `consumo` (o long polling de 20 s não
  ocupa o agendador do Spring), até 10 mensagens por receive.
  - sucesso → `DeleteMessage`;
  - exceção → a mensagem fica e volta depois do `VisibilityTimeout` (60 s); as seguintes do mesmo
    grupo no lote não são processadas, para não inverter a ordem da OS. Na 5ª falha o SQS a move
    para a DLQ;
  - fora do contrato ou sem manipulador → direto para a DLQ (com o atributo `erro`), sem tentar de
    novo.

Métricas: `oficina.mensageria.publicadas` / `oficina.mensageria.falhas` (outbox), `oficina.mensageria.consumidas`
(consumidor) e `oficina.mensageria.sqs.recebidas{resultado=falha|dlq}` (receptor).

## LocalStack no docker-compose

```sh
docker-compose up -d localstack     # cria as 3 filas FIFO + DLQs (scripts/localstack/criar-filas.sh)
```

A imagem é fixada em `localstack/localstack:4.14.0` — as versões 2026.x exigem token de conta
(achado do spike). O `api` do docker-compose já sobe com `billing-commands` pelo SQS e lendo
`os-saga-replies`. O Billing roda a partir do repositório dele, apontando para o mesmo LocalStack e
para o catálogo de peças deste serviço:

```sh
docker-compose up -d                  # db, localstack, mailhog e api (porta 8080)
cd ../oficina-billing-service
docker-compose up -d                  # Postgres do Billing (porta 5434)
SERVER_PORT=8081 PAGAMENTO_GATEWAY=simulado MENSAGERIA_SQS_HABILITADO=true \
  AWS_ENDPOINT_URL_SQS=http://localhost:4566 AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
  CATALOGO_PECAS_URL=http://localhost:8080 SERVICE_TOKEN=token-de-servico-local ./mvnw spring-boot:run
```

O `SERVICE_TOKEN` precisa ser o mesmo nos dois lados (o do compose é `token-de-servico-local`).

Conferir as filas:

```sh
docker exec oficina_localstack awslocal sqs list-queues
docker exec oficina_localstack awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/oficina-local-billing-commands.fifo \
  --attribute-names All --message-attribute-names All
```

## Testes

- `TransporteSqsLocalStackTest`: envio, recebimento, duplicata, DLQ e falha segurando o grupo,
  contra o LocalStack em Testcontainers (Docker precisa estar rodando).
- `ContratoDasMensagensDaSagaTest`: toda mensagem que o OS produz é validada contra o JSON
  Schema do seu `type` em [`../contratos/schemas/`](../contratos/schemas/) — e também as do
  `BillingSimulado`, o dublê que faz o papel do Billing nos testes do OS (orquestrador, BDD,
  controller). O Billing de verdade valida as próprias mensagens no repositório dele.
