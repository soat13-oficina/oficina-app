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
| `CadastrarNovoOrcamentoUseCase` | Interno (chamado por `ordemservico`) | Cria um orçamento a partir dos dados do diagnóstico da OS |
| `ConsultarOrcamentoPorNumeroUseCase` | `GET /orcamentos/{numeroOrcamento}` | Retorna os dados completos de um orçamento pelo número |
| `DecidirOrcamentoExternamenteUseCase` | `POST /integracoes/orcamentos/{numeroOrcamento}/decisao` | Registra a decisão (aprovação ou rejeição) via webhook externo |

### Métodos de Domínio em `Orcamento`

| Método | Transição | Guarda | Efeito |
|---|---|---|---|
| `enviarParaAprovacao()` | `AGUARDANDO_APROVACAO → EM_APROVACAO` | Status deve ser `AGUARDANDO_APROVACAO` | Marca o orçamento como aguardando decisão do cliente |
| `aprovar()` | `EM_APROVACAO → APROVADO` | Status deve ser `EM_APROVACAO` | Aprova o orçamento; libera execução do serviço |
| `rejeitar()` | `EM_APROVACAO → REJEITADO` | Status deve ser `EM_APROVACAO` | Rejeita o orçamento; encerra a OS |

### Cálculo de Valor

| Campo | Fórmula |
|---|---|
| `valorPecas` | `sum(peca.precoUnitario × peca.quantidade)` para cada `PecaOrcamento` |
| `valorTotal` | `valorMaoDeObra + valorPecas - desconto` |

### Regras de Negócio

1. **Snapshot de dados** — `clienteNome`, `clienteCpf`, `placaVeiculo`, `marcaVeiculo` e
   `modeloVeiculo` são copiados da OS no momento da criação e não são alterados mesmo que
   o cadastro original mude.
2. **Idempotência de decisão** — se o webhook enviar a mesma decisão que já foi registrada,
   o `DecidirOrcamentoExternamenteService` retorna o estado atual sem erro.
3. **Conflito de decisão** — se o webhook enviar uma decisão diferente da já registrada, é
   lançada `RegraDeNegocioException`.
4. **Número único** — o número do orçamento é gerado como UUID no momento da criação.
5. **Peças do orçamento** — `PecaOrcamento` é um value object embutido na OS; contém
   referência à `PecaInsumo` (id + nome) e quantidade + preço unitário no momento do
   orçamento.

## Ciclo de Vida

```
[Orçamento criado por CadastrarNovoOrcamento]
              |
              | (chamado por EnviarDiagnosticoParaOrcamentoService)
              ▼
    [AGUARDANDO_APROVACAO]
              |
              | enviarParaAprovacao()
              ▼
       [EM_APROVACAO]
              |
     ┌────────┴────────┐
     |                 |
  aprovar()        rejeitar()
     |                 |
     ▼                 ▼
[APROVADO]        [REJEITADO]
     |                 |
     ▼                 ▼
 [Execução do     [OS finalizada com
  serviço inicia]  MotivoEncerramento.ORCAMENTO_RECUSADO]
```

## Dependências Internas

| Módulo | Direção | Detalhe |
|---|---|---|
| `ordemservico` | `ordemservico` cria e consulta `orcamento` | `EnviarDiagnosticoParaOrcamentoService` chama `CadastrarNovoOrcamentoUseCase`; `FinalizarOrdemDeServicoService` consulta o orçamento para consumir peças |
| `ordemservico` | `orcamento` publica evento consumido por `ordemservico` | `DecidirOrcamentoExternamenteService` publica `StatusOrdemDeServicoAlterado` após registrar a decisão |
| `pecainsumo` | `ordemservico` consome `pecainsumo` via orçamento | `FinalizarOrdemDeServicoService` itera `orcamento.getPecasOrcamento()` e chama `ConsumirPecaUseCase` para cada peça |

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
| 200 OK | Decisão registrada; retorna `OrcamentoResponse` atualizado |
| 400 Bad Request | Conflito de decisão (decisão diferente da já registrada) |
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
| `AGUARDANDO_APROVACAO` | Orçamento criado, aguarda envio para aprovação |
| `EM_APROVACAO` | Enviado ao cliente, aguardando decisão |
| `APROVADO` | Cliente aprovou |
| `REJEITADO` | Cliente rejeitou |

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
| `orcamento/domain/repository/OrcamentoRepository.java` | Port de persistência |
| `orcamento/application/service/CadastrarNovoOrcamentoService.java` | Cria o orçamento; calcula valores; persiste |
| `orcamento/application/service/DecidirOrcamentoExternamenteService.java` | Lógica de idempotência; publica evento de mudança de status |
| `orcamento/infrastructure/web/WebhookDecisaoOrcamentoController.java` | Endpoint público de webhook |

## Observações

- **Preço das peças no orçamento é snapshot**: `precoUnitario` em `PecaOrcamento` é o
  preço da peça no momento da criação do orçamento — variações de preço na `PecaInsumo`
  após o orçamento não afetam o valor já calculado.
- **Relação com `OrdemDeServico`**: o orçamento referencia a OS pelo `numeroOrdemServico`
  (string), não por FK direta. A OS é localizada via `OrdemDeServicoRepository` nos
  services que precisam dela.
- **Webhook é rota pública**: `POST /integracoes/orcamentos/*/decisao` não exige JWT —
  protegida exclusivamente pelo header `X-Webhook-Token` validado pelo `WebhookTokenFilter`.

---

## Pontos de Atenção

### Achados de Integridade Crítica

#### [CRI-001] Ausência de validação de transição antes de `enviarParaAprovacao()`

**Componente afetado**: `Orcamento#enviarParaAprovacao()` · `DecidirOrcamentoExternamenteService`

**Descrição**: o método `enviarParaAprovacao()` transiciona o orçamento de
`AGUARDANDO_APROVACAO` para `EM_APROVACAO`. Se o webhook de decisão chegar antes que
`enviarParaAprovacao()` seja chamado (orçamento ainda em `AGUARDANDO_APROVACAO`), a
chamada a `orcamento.aprovar()` ou `orcamento.rejeitar()` lança `RegraDeNegocioException`
("Orcamento não está em aprovação"), retornando HTTP 400 — não HTTP 422/409. O contrato
de erro não está documentado no Swagger do webhook.

**Impacto**: integrações externas que enviam a decisão imediatamente após a criação do
orçamento recebem um 400 sem contexto suficiente para distinguir conflito de decisão de
chamada fora de ordem.

**Correção sugerida**: documentar a restrição de sequência no contrato OpenAPI do endpoint
de webhook. Avaliar se `enviarParaAprovacao()` deve ser chamado automaticamente na criação,
eliminando o estado intermediário `AGUARDANDO_APROVACAO`.

---

#### [CRI-002] Propagação de evento na decisão sem garantia de atomicidade

**Componente afetado**: `DecidirOrcamentoExternamenteService`

**Descrição**: o service salva o orçamento e em seguida publica `StatusOrdemDeServicoAlterado`
via `ApplicationEventPublisher`. Se a publicação falhar (ex.: erro na atualização da OS no
listener do evento), o orçamento já estará persistido mas o evento não terá sido processado.
Não há controle transacional entre a persistência e o efeito lateral da publicação do evento.

**Impacto**: inconsistência entre o status do orçamento e o status da OS em caso de falha
parcial.

**Correção sugerida**: garantir que a publicação do evento ocorra dentro da mesma transação
JPA da persistência do orçamento (usar `@Transactional` no método de decisão), ou migrar
para eventos transacionais (`@TransactionalEventListener`).

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
