# Domínio: Orcamento

## Objetivo

Representar o acordo financeiro entre a oficina e o cliente para a execução de um serviço
em uma ordem de serviço. Encapsula o cálculo de valor, a lista de peças necessárias e o
processo de aprovação ou rejeição pelo cliente — incluindo o fluxo de decisão via webhook
externo.

## Responsabilidade Principal

Criar, calcular e registrar a decisão de aprovação ou rejeição de orçamentos gerados a
partir do diagnóstico de ordens de serviço. O orçamento é um snapshot imutável no momento
da criação — armazena os dados do cliente e do veículo copiados da OS, sem dependência
posterior dos registros originais.

## Funcionalidades Implementadas

### Use Cases

| Use Case | Endpoint | Descrição |
|---|---|---|
| `CadastrarNovoOrcamentoUseCase` | Interno (comando `GerarOrcamento` da Saga) | Cria o orçamento a partir do snapshot do diagnóstico que vem no comando; preço e descrição das peças pela porta `CatalogoPecas` |
| `ConsultarOrcamentoUseCase` | `GET /orcamentos/{numeroOrcamento}` | Retorna os dados completos de um orçamento pelo número |
| `AprovarOrcamentoUseCase` / `RejeitarOrcamentoUseCase` | `POST /orcamentos/{numero}/aprovacao` · `/rejeicao` | Decisão do cliente (JWT); só publica o evento da Saga — **não reserva peças** (Fase 4) |
| `DecidirOrcamentoExternamenteUseCase` | `POST /integracoes/orcamentos/{numeroOrcamento}/decisao` | Registra a decisão (aprovação ou rejeição) via webhook externo |
| `AlterarOrcamentoUseCase` / `ExcluirOrcamentoUseCase` | `PUT` / `DELETE /orcamentos/{numero}` | Manutenção do orçamento aguardando aprovação |

### Participante da Saga (Fase 4, futuro Billing Service)

Comandos recebidos em `billing-commands` (`infrastructure/mensageria/ManipuladorComandosOrcamento`),
respostas em `os-saga-replies` com `causationId` = o comando. Contratos em `docs/contratos/mensagens.md`.

| Comando | Classe | Resposta |
|---|---|---|
| `GerarOrcamento` | `application/saga/GerarOrcamentoPelaSaga` | `OrcamentoGerado` (id, número, valores, validade) ou `OrcamentoFalhou` (motivo + `pecasInvalidas` quando há peça fora do cadastro). Sem `validade`, vale `orcamento.validade-padrao` (`P7D`); sem `valorMaoDeObra` total, soma a mão de obra dos serviços |
| `ReverterAprovacao` | `application/saga/ReverterAprovacaoPelaSaga` | `AprovacaoRevertida` — faltou estoque na reserva: `APROVADO → AGUARDANDO_APROVACAO` |
| `CancelarOrcamento` | `application/saga/CancelarOrcamentoPelaSaga` | `OrcamentoCancelado` (com `pagamentoCancelado`) — prazo de decisão vencido ou pagamento recusado/expirado; leva junto a cobrança pendente (porta `PagamentosDoOrcamento`, módulo `pagamento`) |

Todos são idempotentes pelo estado: comando reenviado por timeout (`messageId` novo) encontra o
orçamento já gerado / revertido / cancelado e só repete a resposta. Falha de negócio não é exceção
para o consumidor: vira `OrcamentoFalhou` e a Saga decide.

### Métodos de Domínio em `Orcamento`

| Método | Transição | Guarda | Efeito |
|---|---|---|---|
| `aprovar()` | `AGUARDANDO_APROVACAO → APROVADO` | Status deve ser `AGUARDANDO_APROVACAO` | Aprova o orçamento; a Saga reserva as peças e pede o pagamento |
| `rejeitar()` | `AGUARDANDO_APROVACAO → REJEITADO` | Status deve ser `AGUARDANDO_APROVACAO` | Rejeita o orçamento; a Saga encerra a OS |
| `reverterAprovacao()` | `APROVADO → AGUARDANDO_APROVACAO` | Aprovado (já aguardando = nada muda) | Compensação `ReverterAprovacao` |
| `cancelar()` | `AGUARDANDO_APROVACAO` ou `APROVADO → CANCELADO` | Já cancelado = nada muda; rejeitado = erro | Compensação `CancelarOrcamento` |

### Cálculo de Valor

| Campo | Fórmula |
|---|---|
| `valorPecas` | `sum(peca.precoUnitario × peca.quantidade)` para cada `PecaOrcamento` |
| `valorTotal` | `valorMaoDeObra + valorPecas - desconto` (desconto maior que o total = `OrcamentoFalhou`) |

### Regras de Negócio

1. **Snapshot de dados** — `clienteNome`, `clienteCpf`, `placaVeiculo`, `marcaVeiculo` e
   `modeloVeiculo` vêm do comando `GerarOrcamento` (snapshot da OS) e não são alterados mesmo que
   o cadastro original mude. O orçamento não consulta cliente nem OS (ADR 0003).
2. **Idempotência de decisão** — se o webhook enviar a mesma decisão que já foi registrada,
   o `DecidirOrcamentoExternamenteService` retorna o estado atual sem erro.
3. **Conflito de decisão** — se o webhook enviar uma decisão diferente da já registrada, é
   lançada `RegraDeNegocioException`.
4. **Um orçamento por OS** — `CadastrarNovoOrcamentoService` recusa um segundo orçamento para a
   mesma OS; o número é `ORC-<uuid>`.
5. **Peças do orçamento** — `PecaOrcamento` guarda id, descrição e preço da peça **no momento do
   orçamento** (consultados pela porta `CatalogoPecas`; no Billing vira REST
   `POST /internal/pecas-insumos/consulta`). Todas as peças fora do cadastro são listadas juntas
   (`PecasNaoEncontradasException`).
6. **Decisão publica evento da Saga** — aprovar e rejeitar (pelo JWT ou pelo webhook) gravam
   `OrcamentoAprovado` / `OrcamentoRejeitado` no outbox **na mesma transação** da decisão, com o
   canal (`CLIENTE`, `FUNCIONARIO` ou `INTEGRACAO`). O orçamento **não altera a OS nem o estoque**:
   a Saga move a OS e manda reservar as peças (ADR 0004).
7. **Saga de origem** — o orçamento guarda o `sagaId` da OS (coluna `saga_id`, V23) para os
   eventos que publica.

## Ciclo de Vida

```
GerarOrcamento (Saga) ──► [AGUARDANDO_APROVACAO] ──► OrcamentoGerado
                               │          ▲
               aprovar()       │          │ reverterAprovacao()  (ReverterAprovacao: estoque insuficiente)
                               ▼          │
                          [APROVADO] ─────┘
                               │
                               └── cancelar() (CancelarOrcamento: pagamento recusado/expirado) ──► [CANCELADO]

[AGUARDANDO_APROVACAO] ── rejeitar() ──► [REJEITADO]            → OrcamentoRejeitado
[AGUARDANDO_APROVACAO] ── cancelar() (prazo de decisão vencido) ──► [CANCELADO] → OrcamentoCancelado
```

## Dependências Internas

| Módulo | Direção | Detalhe |
|---|---|---|
| `ordemservico` (Saga) | **só por mensagens** | Consome `GerarOrcamento`, `ReverterAprovacao` e `CancelarOrcamento` (fila `billing-commands`); publica `OrcamentoGerado`, `OrcamentoFalhou`, `OrcamentoAprovado`, `OrcamentoRejeitado`, `AprovacaoRevertida` e `OrcamentoCancelado` (fila `os-saga-replies`) |
| `pagamento` | mesmo serviço (Billing) | Porta `PagamentosDoOrcamento`: o cancelamento do orçamento cancela a cobrança pendente; o `pagamento` lê o orçamento (valor) e o cancela no estorno |
| `pecainsumo` | porta `CatalogoPecas` | Preço e descrição das peças na criação, via `br.com.oficina.ponte.CatalogoDePecasDoEstoque` (sem import do estoque) |

## Dependências Externas

- **Sistema externo (cliente)** — envia a decisão de aprovação/rejeição via `POST /integracoes/orcamentos/{numero}/decisao` usando o header `X-Webhook-Token`

## Pontos de Entrada REST

Todos os endpoints abaixo exigem autenticação, exceto o webhook.

---

### `GET /orcamentos/{numeroOrcamento}` — Consultar orçamento

**Path param**: `numeroOrcamento` (UUID como String).

**Respostas**:

| Status | Situação |
|---|---|
| 200 OK | `OrcamentoResponse` com dados completos |
| 404 Not Found | Orçamento não encontrado |

---

### `POST /integracoes/orcamentos/{numeroOrcamento}/decisao` — Decisão de orçamento (webhook)

**Autenticação**: header `X-Webhook-Token` (sem JWT).

**Request body** (`DecisaoOrcamentoRequest`):

| Campo | Tipo | Descrição |
|---|---|---|
| `decisao` | `DecisaoOrcamento` | `APROVADO` ou `REJEITADO` |

**Respostas**:

| Status | Situação |
|---|---|
| 200 OK | Decisão registrada (ou idêntica à já registrada); retorna `DecisaoOrcamentoResponse` com `numeroOrcamento`, `statusOrcamento` e `ordemServicoId`. **Fase 4:** a situação da OS não vem mais na resposta — a Saga move a OS logo depois (consulte `GET /ordens-servico/{numero}` ou `GET /sagas/{ordemServicoId}`) |
| 400 Bad Request | Orçamento fora de `AGUARDANDO_APROVACAO` (por exemplo, `CANCELADO`) |
| 409 Conflict | Conflito de decisão (decisão diferente da já registrada) |
| 401 Unauthorized | Token de webhook ausente ou inválido |
| 404 Not Found | Orçamento não encontrado |

## Modelos de Domínio

### `Orcamento` — Aggregate Root

| Campo | Tipo | Descrição |
|---|---|---|
| `numero` | `String` (UUID) | Identificador único do orçamento |
| `status` | `StatusOrcamento` | Estado atual do orçamento |
| `numeroOrdemServico` | `String` | Número da OS associada |
| `clienteNome` | `String` | Snapshot do nome do cliente |
| `clienteCpf` | `String` | Snapshot do CPF/CNPJ do cliente |
| `placaVeiculo` | `String` | Snapshot da placa do veículo |
| `marcaVeiculo` | `String` | Snapshot da marca do veículo |
| `modeloVeiculo` | `String` | Snapshot do modelo do veículo |
| `valorMaoDeObra` | `BigDecimal` | Valor cobrado por mão de obra |
| `valorPecas` | `BigDecimal` | Calculado automaticamente (`sum(peças)`) |
| `desconto` | `BigDecimal` | Desconto aplicado (pode ser zero) |
| `valorTotal` | `BigDecimal` | `valorMaoDeObra + valorPecas - desconto` |
| `pecasOrcamento` | `List<PecaOrcamento>` | Peças necessárias com qtd. e preço unitário |
| `criadoEm` | `LocalDateTime` | Timestamp de criação |

### `StatusOrcamento` — Enum

| Valor | Descrição |
|---|---|
| `AGUARDANDO_APROVACAO` | Orçamento gerado pela Saga, aguardando a decisão do cliente (também depois de uma aprovação revertida) |
| `APROVADO` | Cliente aprovou |
| `REJEITADO` | Cliente rejeitou |
| `CANCELADO` | Compensação da Saga: prazo de decisão vencido, pagamento recusado/expirado ou execução abortada (estorno) — Fase 4 |

### `PecaOrcamento` — Value Object (`@Embeddable`)

| Campo | Tipo | Descrição |
|---|---|---|
| `pecaInsumoId` | `String` | ID da `PecaInsumo` referenciada |
| `nomePeca` | `String` | Nome da peça no momento do orçamento (snapshot) |
| `quantidade` | `Integer` | Quantidade necessária |
| `precoUnitario` | `BigDecimal` | Preço unitário no momento do orçamento |

### `DecisaoOrcamento` — Enum

| Valor | Mapeamento |
|---|---|
| `APROVADO` | Chama `orcamento.aprovar()` |
| `REJEITADO` | Chama `orcamento.rejeitar()` |

## Arquivos Críticos

| Arquivo | Responsabilidade |
|---|---|
| `orcamento/domain/model/Orcamento.java` | Aggregate root; cálculo de `valorTotal`; máquina de estados |
| `orcamento/domain/model/PecaOrcamento.java` | Value object embutido; snapshot de peça |
| `orcamento/domain/model/StatusOrcamento.java` | Enum dos 4 estados do orçamento |
| `orcamento/application/saga/` | Participante da Saga: `GerarOrcamento`, `ReverterAprovacao`, `CancelarOrcamento` e os eventos publicados |
| `orcamento/application/port/` | Portas `CatalogoPecas` (estoque) e `PagamentosDoOrcamento` (pagamento) |
| `orcamento/domain/repository/OrcamentoRepository.java` | Port de persistência |
| `orcamento/application/service/CadastrarNovoOrcamentoService.java` | Cria o orçamento; calcula valores; persiste |
| `orcamento/application/service/DecidirOrcamentoExternamenteService.java` | Lógica de idempotência; publica evento de mudança de status |
| `orcamento/infrastructure/web/WebhookDecisaoOrcamentoController.java` | Endpoint público de webhook |

## Observações

- **Preço das peças no orçamento é snapshot**: `precoUnitario` em `PecaOrcamento` é o
  preço da peça no momento da criação do orçamento — variações de preço na `PecaInsumo`
  após o orçamento não afetam o valor já calculado.
- **Relação com `OrdemDeServico`**: o orçamento guarda o `ordemDeServicoId` e o `sagaId`, mas não
  importa nada do módulo `ordemservico` (regra ArchUnit) — tudo o que precisa da OS chega no
  comando `GerarOrcamento`.
- **Webhook é rota pública**: `POST /integracoes/orcamentos/*/decisao` não exige JWT —
  protegida exclusivamente pelo header `X-Webhook-Token` validado pelo `WebhookTokenFilter`.

---

## Pontos de Atenção

### Achados de Integridade Crítica

> **Curadoria (spec `006-orcamento-achados-criticos`)**: CRI-002 (atomicidade) foi corrigido; o
> resíduo válido de CRI-001 (contrato de erro do webhook) foi corrigido. A premissa original de CRI-001
> (estado intermediário `EM_APROVACAO`) já era **obsoleta** — esse estado não existe no enum
> `StatusOrcamento` (`AGUARDANDO_APROVACAO`, `APROVADO`, `REJEITADO`). Ambos os achados estão **resolvidos**.

#### [CRI-001] Contrato de erro do webhook de decisão ✅ RESOLVIDO

**Componente afetado**: `WebhookDecisaoOrcamentoController` · `DecidirOrcamentoExternamenteService`

**Descrição**: a premissa original (estado `EM_APROVACAO` travado) já não existia — `aprovar()`/`rejeitar()`
exigem `AGUARDANDO_APROVACAO` e a decisão externa já trata chamadas repetidas de forma idempotente. O resíduo
real era o contrato de erro do webhook: não documentado, e decisão divergente de uma já registrada retornava
HTTP 400.

**Correção aplicada (spec `006`)**: decisão divergente passou a lançar `ConflitoDeRecursoException`, mapeada
para **HTTP 409 Conflict** no `ApiExceptionHandler`. Decisão idêntica permanece idempotente (200). O endpoint
ganhou `WebhookDecisaoOrcamentoControllerSwagger` documentando 200/400/404/409.

**Estado**: resolvido.

---

#### [CRI-002] Atomicidade da decisão externa ✅ RESOLVIDO

**Componente afetado**: `DecidirOrcamentoExternamenteService`

**Descrição**: a decisão externa aprovava/rejeitava o orçamento (reservando peças), atualizava a OS e
publicava o evento **sem fronteira transacional** — uma falha técnica parcial deixava estado inconsistente.

**Correção aplicada (spec `006`)**: `decidir()` passou a ser `@Transactional`. Como os métodos de repositório
envolvidos já usam propagação `REQUIRED`, a aprovação/rejeição do orçamento, a reserva de peças e a
atualização da OS **confirmam juntas ou revertem juntas**. A notificação permanece `@Async` (best-effort),
fora da transação. A compensação de reserva por estoque insuficiente foi preservada.

**Fase 4**: a OS e a reserva de peças saíram desta transação. Agora confirmam juntos a decisão do
orçamento e o evento da Saga no outbox (`DecidirOrcamentoAtomicidadeIntegrationTest`); a Saga reserva
as peças (`ReservarPecas`, tudo ou nada) e move a OS ao consumir o evento. Estoque insuficiente vira a
compensação `ReverterAprovacao`.

**Estado**: resolvido.

---

### Achados de Melhoria

#### [MEL-001] Anotações JPA no aggregate root — dívida técnica arquitetural

**Componente afetado**: `Orcamento.java`

**Descrição**: `Orcamento` possui `@Entity`, `@Table`, `@Embedded` e `@ElementCollection`
diretamente no aggregate root, violando o Princípio I da constituição.

**Sugestão de backlog**: extrair `OrcamentoJpaEntity` com `fromDomain()` / `toDomain()`,
seguindo o padrão `PecaInsumo` / `PecaInsumoJpaEntity`.

---

#### [MEL-002] Ausência de paginação no endpoint de consulta

**Componente afetado**: `GET /orcamentos` (se implementado no futuro)

**Descrição**: não há endpoint de listagem de orçamentos. Quando implementado, deve incluir
paginação desde o início para evitar a dívida de in-memory filtering presente em outros módulos.

**Sugestão de backlog**: implementar `GET /orcamentos` com `Pageable` e filtros por status,
numero da OS e data de criação.

---

#### [MEL-003] Desconto sem limite máximo validado

**Componente afetado**: `Orcamento` (construtor / `valorTotal`)

**Descrição**: o campo `desconto` é aceito sem validação de limite superior — um desconto
maior que `valorMaoDeObra + valorPecas` resulta em `valorTotal` negativo, que é persistido
sem erro.

**Sugestão de backlog**: adicionar invariante no domínio: `desconto <= valorMaoDeObra + valorPecas`,
lançando `RegraDeNegocioException` se violado.
