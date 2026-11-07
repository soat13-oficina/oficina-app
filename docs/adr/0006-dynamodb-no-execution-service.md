# ADR 0006 — DynamoDB como banco do Execution Service

- **Status:** Proposto
- **Data:** 2026-10-15
- **Contexto:** Tech Challenge SOAT 13 — Fase 4

## Contexto

O enunciado exige **pelo menos um banco SQL e um NoSQL** entre os microsserviços.
OS e Billing ficam no PostgreSQL ([ADR 0003](0003-divisao-em-microsservicos-e-dono-dos-dados.md)):
o OS já tem o modelo relacional maduro da Fase 3 e o Billing lida com dinheiro.
Sobra a Execution — fila de diagnóstico e execução, diagnóstico, estoque de peças
e reservas — e a pergunta é se o domínio dela combina com um banco NoSQL ou se a
escolha seria só para cumprir tabela.

Padrões de acesso da Execution:

| Operação | Chave de acesso |
|---|---|
| Ler/atualizar a execução de uma OS | `ordemServicoId` |
| Listar a fila (aguardando diagnóstico / execução) em ordem de chegada | etapa + data de criação |
| Ler/atualizar uma peça, consultar preços de uma lista de peças | `pecaId` |
| Reservar, consumir e liberar as peças de uma OS — **atomicamente** | `ordemServicoId` + `pecaId` |
| Listar o catálogo | varredura de uma tabela pequena |

Não há consulta ad hoc nem junção: tudo é acesso por chave ou por um índice conhecido.

## Decisão

**Amazon DynamoDB, on-demand, com quatro tabelas por ambiente.**

| Tabela | Chave | Atributos principais | Índices |
|---|---|---|---|
| `oficina-<env>-execucoes` | PK `ordemServicoId` | `numeroOrdemServico`, `etapa`, veículo (snapshot), descrição do problema, mecânico, diagnóstico (serviços com mão de obra, peças), histórico de etapas, `criadoEm`, `atualizadoEm`, `versao` | GSI `fila-por-etapa` (PK `etapa`, SK `criadoEm`) — **a fila** |
| `oficina-<env>-pecas` | PK `pecaId` | descrição, marca, preço, código de referência, categoria, `quantidadeEstoque`, `quantidadeReservada`, **`quantidadeDisponivel`**, `versao` | — |
| `oficina-<env>-reservas` | PK `ordemServicoId`, SK `pecaId` | quantidade, status (`RESERVADA`, `CONSUMIDA`, `LIBERADA`) | — |
| `oficina-<env>-execution-controle` | PK `pk` (`MSG#<messageId>` ou `OUTBOX#<messageId>`) | idempotência e outbox; `expiraEm` (TTL de 7 dias) | GSI esparso `outbox-pendente` (PK `pendente`, SK `criadoEm`) |

Etapas da execução: `AGUARDANDO_DIAGNOSTICO`, `EM_DIAGNOSTICO`,
`DIAGNOSTICO_CONCLUIDO`, `AGUARDANDO_EXECUCAO`, `EM_EXECUCAO`, `CONCLUIDA`,
`ABORTADA`, `CANCELADA`.

### Estoque: `quantidadeDisponivel` explícita

O plano original previa a condição `quantidadeEstoque - quantidadeReservada >= :q`.
**O DynamoDB não aceita aritmética em `ConditionExpression`** — o spike confirmou
que a requisição é rejeitada. Por isso a peça guarda `quantidadeDisponivel`, mantida
em toda escrita que mexe em estoque ou reserva:

| Operação | `UpdateExpression` | Condição |
|---|---|---|
| Reservar `q` | `disponivel -= q`, `reservada += q` | `quantidadeDisponivel >= :q` |
| Liberar reserva | `disponivel += q`, `reservada -= q` | reserva `RESERVADA` |
| Consumir na conclusão | `estoque -= q`, `reservada -= q` | reserva `RESERVADA` |
| Entrada de estoque | `estoque += q`, `disponivel += q` | — |
| Saída manual | `estoque -= q`, `disponivel -= q` | `quantidadeDisponivel >= :q` |

Invariante `disponivel = estoque − reservada`, coberta por teste unitário do adapter.

### Reserva atômica

Um `TransactWriteItems` por comando `ReservarPecas`:

1. para cada peça, `Update` com a condição `quantidadeDisponivel >= :q`;
2. `Put` de cada reserva;
3. `Put` de idempotência `MSG#<messageId>` com `attribute_not_exists(pk)`;
4. `Put` do evento `PecasReservadas` no outbox (`OUTBOX#<id>`, `pendente = "1"`).

Se a transação é cancelada, `CancellationReasons` diz **qual item** falhou: condição
de peça → lê as peças e publica `ReservaFalhou` com as faltantes; condição de
idempotência → mensagem repetida, republica a resposta a partir do estado atual
([ADR 0005](0005-sqs-fifo-outbox-e-idempotencia.md)). Limite de 100 itens por
transação ⇒ até 49 peças distintas por OS, folgado para o domínio.

### Outbox no DynamoDB

O item de outbox entra na mesma transação da mudança de estado com o atributo
`pendente`. O publicador consulta o GSI esparso `outbox-pendente`, envia ao SQS e
faz `REMOVE pendente` — o item sai do índice e fica para auditoria até o TTL.

### Acesso

- **Enhanced Client** (`DynamoDbEnhancedClient`, beans anotados) para CRUD e leituras por chave; `@DynamoDbVersionAttribute` para lock otimista em `execucoes` e `pecas`.
- **Cliente de baixo nível** para as transações com condição — a expressão fica explícita e testável.
- Leitura por chave com consistência forte; a fila (GSI) é eventualmente consistente, aceitável para uma listagem.
- Sem JPA nem Flyway no serviço: as tabelas são criadas pelo Terraform do `infra/` da Execution; o catálogo de demonstração é carregado por `scripts/seed-pecas.json` + `aws dynamodb batch-write-item` no pipeline de hml.

### Evidência do spike (Passo 0)

Contra `localstack/localstack:4.14.0`, com AWS SDK v2 2.46.6:

- Enhanced Client grava e lê o bean `Peca` com `@DynamoDbBean`;
- condição com aritmética é rejeitada (`DynamoDbException`);
- reserva com `quantidadeDisponivel` debita e reserva atomicamente;
- a mesma mensagem reenviada falha só no item de idempotência (`CancellationReasons = [None, ConditionalCheckFailed]`) e não altera o estoque;
- quantidade acima do disponível falha só no item da peça (`[ConditionalCheckFailed, None]`) e não altera nada.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| **PostgreSQL também na Execution** | Atenderia o domínio, mas não cumpre "1 SQL + 1 NoSQL", e o Billing — que lida com dinheiro — é o candidato natural ao relacional. |
| **DynamoDB no Billing** | Orçamento × pagamentos × estornos pedem constraints e consultas relacionais; e o Mercado Pago exige rastreabilidade transacional. |
| **MongoDB (Atlas) / Amazon DocumentDB** | Documento combina com o diagnóstico, mas DocumentDB cobra instância por hora (caro para subir e descer) e Atlas fica fora da AWS, fora do Terraform e do IRSA. |
| **Redis / ElastiCache** | Bom para cache e fila efêmera, não como banco de registro do estoque. |
| **Uma tabela só (single-table design)** | Menos tabelas, mas modelagem mais difícil de explicar e de testar num projeto acadêmico; quatro tabelas com nomes de domínio documentam melhor a decisão. |

## Consequências

**Positivas**

- Cumpre o requisito NoSQL com justificativa de domínio: tudo é acesso por chave.
- Sem servidor para operar, criado e destruído pelo Terraform junto com o ambiente; custo de centavos no volume da demo.
- Reserva de estoque atômica e idempotente numa única chamada.

**Negativas e mitigações**

- **Sem consulta ad hoc nem relatório SQL.** Aceito: relatórios de negócio ficam no OS; a Execution só precisa da fila e do estoque.
- **GSI `fila-por-etapa` concentra escrita numa partição por etapa.** Irrelevante no volume da oficina; se crescesse, sufixo de shard na chave.
- **`quantidadeDisponivel` é dado derivado.** Mantido em todas as escritas e coberto por teste da invariante.
- **Emulador local:** LocalStack fixado em `4.14.0` (as imagens `2026.x` exigem `LOCALSTACK_AUTH_TOKEN`); plano B `amazon/dynamodb-local`.

## Relacionado

- [ADR 0003](0003-divisao-em-microsservicos-e-dono-dos-dados.md) — divisão e dono dos dados
- [ADR 0005](0005-sqs-fifo-outbox-e-idempotencia.md) — outbox e idempotência
- [`../spikes/0001-compatibilidade-fase4.md`](../spikes/0001-compatibilidade-fase4.md) — resultado do spike
