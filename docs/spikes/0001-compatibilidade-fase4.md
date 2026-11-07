# Spike 0001 — Compatibilidade das bibliotecas da Fase 4

- **Data:** 2026-10-15
- **Objetivo:** confirmar, antes de escrever código de produção, que as bibliotecas que a Fase 4 pretende usar convivem com a base do `oficina-app` — e achar cedo o que não funciona como o plano supunha.
- **Resultado:** **viável**, com três achados que mudaram decisões (abaixo).
- **Código:** projeto descartável `spike-fase4` (fora dos repositórios de entrega); os trechos que importam estão reproduzidos aqui.

## Base testada

| Item | Versão |
|---|---|
| Java | 21.0.12 |
| Spring Boot / Spring Framework | 4.0.5 / 7.0.6 |
| JUnit (gerenciado pelo Boot) | Jupiter / Platform 6.0.3 |
| Jackson (gerenciado pelo Boot) | 3.1.0 (`tools.jackson`) |
| AWS SDK v2 (`sqs`, `dynamodb-enhanced`) | 2.46.6 *(mesma do `oficina-app`)* |
| Testcontainers (`localstack`) | 1.21.4 *(mesma do `oficina-app`)* |
| LocalStack (imagem) | `localstack/localstack:4.14.0` |
| Cucumber (`cucumber-java`, `cucumber-spring`, `cucumber-junit-platform-engine`) | 8.0.4 |
| Mercado Pago `com.mercadopago:sdk-java` | 3.7.0 |
| ArchUnit (`archunit-junit5`) | 1.5.1 |
| Awaitility | 4.3.0 |

## Resultado por item

| # | Verificação | Resultado |
|---|---|---|
| 1 | Cucumber 8 + `cucumber-spring` subindo `@SpringBootTest` (Spring 7), feature em português (`# language: pt`), Awaitility nos passos | OK |
| 2 | ArchUnit 1.5 em Java 21 / JUnit 6 | OK |
| 3 | Jackson 3: envelope com `UUID` e `Instant` (ISO-8601) ida e volta, sem configuração | OK |
| 4 | SQS FIFO no LocalStack: redrive para DLQ, ordem por `MessageGroupId`, deduplicação por `MessageDeduplicationId`, `type` como *message attribute* | OK |
| 5 | DynamoDB no LocalStack: Enhanced Client (`@DynamoDbBean`), `TransactWriteItems` com condição e idempotência | OK, com **achado A** |
| 6 | SDK do Mercado Pago: preferência e estorno sobre `MPHttpClient` injetado (sem rede) | OK, com **achado B** |
| 7 | Webhook do MP: manifesto `id:{data.id};request-id:{x-request-id};ts:{ts};` × `WebhookSignatureValidator` do SDK | OK |
| 8 | LocalStack na imagem mais recente (`2026.10.0`) | Falhou — **achado C** |

Execução final: `Tests run: 10, Failures: 0, Errors: 0` — `BUILD SUCCESS`.

## Achados

### A — DynamoDB não aceita aritmética em `ConditionExpression`

A condição prevista no plano, `quantidadeEstoque - quantidadeReservada >= :q`, é
rejeitada (`DynamoDbException`). Solução adotada: a peça guarda
`quantidadeDisponivel`, mantida em toda escrita
([ADR 0006](../adr/0006-dynamodb-no-execution-service.md)):

```java
Update.builder()
    .tableName("oficina-hml-pecas")
    .key(Map.of("pecaId", AttributeValue.fromS(pecaId)))
    .updateExpression("SET quantidadeDisponivel = quantidadeDisponivel - :q, quantidadeReservada = quantidadeReservada + :q")
    .conditionExpression("quantidadeDisponivel >= :q")
    .expressionAttributeValues(Map.of(":q", AttributeValue.fromN(Integer.toString(quantidade))))
    .build();
```

Com o `Put` de idempotência (`attribute_not_exists(pk)`) na mesma transação,
`TransactionCanceledException.cancellationReasons()` diz qual item falhou:
`[ConditionalCheckFailed, None]` = estoque insuficiente; `[None, ConditionalCheckFailed]`
= mensagem repetida. Nos dois casos nada é alterado.

### B — SDK do Mercado Pago envia duas chaves de idempotência

O SDK 3.7.0 **sempre** gera a própria `X-Idempotency-Key` em POST e acrescenta a
informada em `MPRequestOptions.customHeaders` com o nome em minúsculas — a requisição
sai com `X-Idempotency-Key` **e** `x-idempotency-key`, com valores diferentes. A chave
persistida pelo Billing (para reenvio seguro depois de uma queda) ficaria sem efeito.

Contorno validado: decorar o `MPHttpClient` (que o SDK aceita no construtor dos
clientes) e descartar a chave gerada quando a nossa existe:

```java
MPHttpClient semChaveGerada = request -> {
    if (request.getHeaders().containsKey("x-idempotency-key")) {
        request.getHeaders().remove("X-Idempotency-Key");
    }
    return delegado.send(request);
};
new PreferenceClient(semChaveGerada).create(pedido, opcoes);
```

O mesmo ponto de injeção permite testar o adapter sem rede (dublê) e com WireMock
(cliente que reescreve a URL), já que a URL base do SDK é constante. Decisão:
**usar o SDK** atrás de uma porta, com esse decorador; `RestClient` direto fica como
plano B ([ADR 0007](../adr/0007-mercado-pago-checkout-pro.md)).

O SDK traz `WebhookSignatureValidator`, que confirmou o manifesto da documentação
(HMAC-SHA256 hexadecimal, `ts` em segundos) e recusa segredo errado, `data.id`
trocado e `ts` fora da tolerância.

### C — LocalStack passou a exigir token nas imagens novas

As imagens `localstack/localstack` agora seguem versão por calendário (`2026.x`) e,
sem `LOCALSTACK_AUTH_TOKEN`, o container encerra com:

```
License activation failed! ... No credentials were found in the environment.
Please set the LOCALSTACK_AUTH_TOKEN variable to a valid auth token.
```

A `4.14.0` (última da linha 4.x) sobe sem token e atende SQS e DynamoDB. Decisão:
**fixar `localstack/localstack:4.14.0`** nos testes e no `docker-compose`; nunca usar
`latest`. Plano B, se a 4.14 deixar de servir: `softwaremill/elasticmq-native` (SQS)
+ `amazon/dynamodb-local` (DynamoDB) — **não testado** neste spike.

## Outras observações

- `cucumber-junit-platform-engine` 8 avisa quando a feature é selecionada com `@SelectClasspathResource` apontando para um pacote; usar `@SelectPackages("features")`.
- O SDK do Mercado Pago traz Apache HttpClient 4.5 e Gson para o classpath; o contexto do Spring Boot 4 subiu normalmente com eles. Ficam restritos ao Billing.
- Comandos `type` e `version` como *message attributes* permitem rotear sem desserializar o corpo — útil para mandar à DLQ mensagens de tipo desconhecido.

## O que **não** foi verificado (fica para os Passos seguintes)

- Propagação de trace do `dd-java-agent` através do SQS FIFO com envelope próprio (Passo 3, com o agent real).
- Conta sandbox do Mercado Pago, usuários de teste e um pagamento real com cartão `APRO`/`OTHE` (depende de credenciais do grupo).
- AWS Load Balancer Controller no EKS (Passo 2, na plataforma).
- `com.networknt:json-schema-validator` para os testes de contrato em Java (os schemas foram validados com `ajv`).
