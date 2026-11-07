# ADR 0003 — Divisão em microsserviços e dono de cada dado

- **Status:** Proposto
- **Data:** 2026-10-15
- **Contexto:** Tech Challenge SOAT 13 — Fase 4

## Contexto

O enunciado da Fase 4 exige **no mínimo três microsserviços independentes**, cada
um com repositório, infraestrutura e banco próprios, pelo menos um banco SQL e um
NoSQL, e a regra de que **nenhum serviço acessa diretamente o banco de outro**.

Hoje o `oficina-app` é um monólito hexagonal com sete módulos
(`auth`, `cliente`, `veiculo`, `ordemservico`, `orcamento`, `pecainsumo`,
`notificacao`) sobre um único RDS PostgreSQL. Os módulos já estão separados em
`domain / application / infrastructure`, mas se acoplam em sete pontos — o pior
deles é `DecidirOrcamentoExternamenteService`, que, de dentro de `orcamento`,
**grava** a ordem de serviço. Fora do monólito, a Lambda `token` do
`oficina-lambda-auth` faz `SELECT` direto na tabela `clientes`.

O enunciado sugere a divisão "OS / Billing / Execution". A pergunta é onde cortar
e quem fica dono de cada tabela.

## Decisão

**Três serviços, divididos por capacidade de negócio: atendimento, cobrança e produção.**

| Serviço | Repositório | Responsabilidade | Banco |
|---|---|---|---|
| **OS Service** (`oficina-os-service`) | `oficina-app` | Abertura, status, histórico e entrega da OS; clientes, veículos, funcionários; login de funcionários; notificações; **orquestrador da Saga** ([ADR 0004](0004-saga-orquestrada.md)) | PostgreSQL — RDS `oficina-db-<env>` (já existe) |
| **Billing Service** (`oficina-billing-service`) | `oficina-billing-service` (novo) | Orçamento (geração, alteração, aprovação, rejeição, webhook de decisão, tokens de integração), pagamento no Mercado Pago, estorno | PostgreSQL — RDS `oficina-billing-db-<env>` (novo) |
| **Execution Service** (`oficina-execution-service`) | `oficina-execution-service` (novo) | Fila de diagnóstico e de execução, diagnóstico, execução, aborto, catálogo e estoque de peças, reservas | DynamoDB ([ADR 0006](0006-dynamodb-no-execution-service.md)) |

O nome lógico do serviço (`oficina-os-service`) vale para `DD_SERVICE`, labels do
Kubernetes, ECR e filas, **independentemente** de o repositório `oficina-app` ser
renomeado — a renomeação é opcional e fica a critério do grupo.

### Dono de cada dado

| Dado | Dono | Observação |
|---|---|---|
| `clientes`, `veiculos`, `funcionarios`, `usuarios`, `notificacoes` | OS | Sem mudança |
| `ordens_de_servico`, `ordem_servico_servicos`, `ordem_servico_pecas` | OS | Serviços e peças passam a ser **snapshot** do diagnóstico recebido por evento ([ADR 0002](0002-snapshot-em-ordens-e-orcamentos.md) estendido) |
| `ordem_servico_historico`, `saga_instancia`, `saga_passos`, `outbox_mensagens`, `mensagens_processadas` *(novas)* | OS | Histórico de status exigido pelo enunciado, estado da Saga, outbox e idempotência |
| `orcamentos`, `orcamento_servicos_propostos`, `orcamento_pecas_previstas`, `tokens_integracao` | Billing | Perdem as FKs para `ordens_de_servico`, `clientes` e `funcionarios`: ficam só os `uuid` + snapshot vindo no comando `GerarOrcamento` |
| `pagamentos`, `outbox_mensagens`, `mensagens_processadas` *(novas)* | Billing | |
| `pecas_insumos` | Execution (tabela DynamoDB `pecas`) | Dados de demonstração recarregados por seed |
| execuções, reservas, controle *(novos)* | Execution | |

Como a infraestrutura é efêmera (apply/destroy sob demanda), não há migração de
dados: migrations Flyway **removem** do RDS do OS o que mudou de dono, a `V1` do
Billing **cria** as tabelas dele e um seed carrega as peças no DynamoDB.

### Como os serviços conversam

- **Assíncrono (padrão):** comandos e eventos da Saga por **Amazon SQS FIFO**, com
  outbox e idempotência — [ADR 0005](0005-sqs-fifo-outbox-e-idempotencia.md).
  Contratos em [`../contratos/mensagens.md`](../contratos/mensagens.md).
- **Síncrono (só quando a resposta é necessária na hora):**

| Chamador → chamado | Endpoint | Por quê |
|---|---|---|
| Billing → Execution | `POST /internal/pecas-insumos/consulta` | O orçamento precisa do preço das peças no momento em que é criado |
| Lambda `token` → OS | `GET /internal/clientes/por-cpf/{cpf}` | O login por CPF precisa da resposta na hora; substitui o `SELECT` direto no RDS |

Chamadas internas: timeout de 2–3 s, retry com backoff só em operação idempotente,
propagação de `x-request-id`, header `X-Service-Token` (segredo
`oficina/<env>/service-token`). `/internal/**` nunca é roteado pelo API Gateway
como rota pública — ver [ADR 0008](0008-exposicao-e-roteamento.md).

### Outras decisões que este ADR fixa

- **Diagnóstico fica na Execution**, como sugere o enunciado. O mecânico informa, ao
  concluir o diagnóstico, os serviços **com o valor de mão de obra de cada um** e as
  peças previstas. O orçamento é gerado automaticamente pela Saga a partir disso
  (desconto zero; validade calculada pelo Billing a partir de configuração). O
  atendente continua podendo ajustar o orçamento (`PUT /orcamentos/{id}`) enquanto
  ele aguarda aprovação.
- **Um namespace por ambiente** (`oficina-hml`, `oficina-prd`) com os três serviços
  dentro; isolamento por ServiceAccount/IRSA e labels. Quotas revisadas no Passo 2.
- **JWT validado em cada serviço** com o mesmo segredo `oficina/<env>/jwt-secret`
  (cópia de `JwtUtil` + filtro em modo só validação). Aceita token de cliente
  (`tipo = CLIENTE`, emitido pela Lambda) e de funcionário (emitido pelo OS).
- **Lambda deixa de ler o banco**: passa a chamar o OS por HTTP, com a flag de
  transição `FONTE_CLIENTE=banco|api` para não quebrar o login durante a troca.

### Ordem de execução

Primeiro desacoplar **dentro do monólito** (porta `MensageriaPort` com adapter em
memória, Saga, ArchUnit proibindo imports entre `ordemservico`, `orcamento` e
`pecainsumo`); depois extrair Billing e, em seguida, Execution, trocando o adapter
em memória por SQS/HTTP. Com o hexagonal que já existe, extrair um serviço vira
mover o pacote e trocar adapters — e os bugs da Saga aparecem num processo só, sem
rede no meio.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| **Diagnóstico no OS** e Execution só com execução e estoque | Menos trabalho, mas contraria a sugestão do enunciado ("Execution: fila, diagnóstico, reparo") e deixa a Execution magra demais para justificar um serviço. Fica como plano de contingência se o prazo apertar. |
| **Estoque num quarto serviço** (Inventory) | Divisão defensável, mas quatro serviços para um grupo pequeno multiplicam pipelines, infraestrutura e custo sem ganho de avaliação. A reserva de peças está acoplada à execução (consome-se o que foi reservado para a OS), o que favorece mantê-las juntas. |
| **Billing dono só do pagamento**, orçamento no OS | O orçamento é o documento que gera a cobrança; separar os dois cria uma Saga a mais (orçamento ↔ pagamento) entre serviços. |
| **Um serviço por módulo do monólito** (7 serviços) | Nanoserviços: cliente, veículo e funcionário não têm ciclo de vida próprio fora da OS. |
| **Lambda continua lendo `clientes`** | Viola diretamente a regra "nenhum serviço acessa o banco de outro". |

## Consequências

**Positivas**

- Cada serviço altera só os próprios dados; a regra de banco exclusivo passa a ser verificável (security groups, IAM e ArchUnit).
- Billing ganha PostgreSQL (ACID para dinheiro) e Execution ganha DynamoDB — cumpre "1 SQL + 1 NoSQL" com justificativa de domínio, não por obrigação.
- A divisão segue o exemplo do enunciado, o que simplifica a defesa no vídeo e no PDF.

**Negativas e mitigações**

- **Consistência eventual** entre OS, orçamento e execução. Mitigado com `GET /sagas/{ordemServicoId}`, histórico da OS e Awaitility nos testes.
- **Dados duplicados** (cliente e veículo no orçamento, diagnóstico na OS). É o snapshot do ADR 0002, agora entre serviços: documento histórico, não projeção.
- **Seis repositórios para manter.** Mitigado copiando o pipeline, o Kustomize e o padrão de Terraform que já funcionam.
- **Login passa a depender do OS no ar.** Aceito; a alternativa (read model de clientes em DynamoDB alimentado por eventos) fica registrada para depois.

## Relacionado

- [ADR 0004](0004-saga-orquestrada.md) — Saga orquestrada
- [ADR 0005](0005-sqs-fifo-outbox-e-idempotencia.md) — mensageria
- [ADR 0006](0006-dynamodb-no-execution-service.md) — DynamoDB na Execution
- [ADR 0007](0007-mercado-pago-checkout-pro.md) — Mercado Pago
- [ADR 0008](0008-exposicao-e-roteamento.md) — exposição e roteamento
- [`../arquitetura/convencoes-de-nomes.md`](../arquitetura/convencoes-de-nomes.md) — nomes de serviços, filas, tabelas e segredos
