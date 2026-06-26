# Domínio: PecaInsumo

## Objetivo

Controlar o estoque de peças e insumos utilizados nos serviços da oficina, permitindo
cadastro, consulta, atualização e controle de reserva e consumo. É o módulo de referência
arquitetural do projeto — é o único que implementa completamente o padrão hexagonal com
domínio puro (sem JPA no aggregate root).

## Responsabilidade Principal

Manter o inventário de peças com controle de quantidade em estoque e quantidade reservada
para orçamentos em andamento. Garante que peças só sejam consumidas se houver reserva
correspondente, e que reservas só sejam feitas se houver estoque disponível.

## Funcionalidades Implementadas

### Use Cases

| Use Case | Endpoint | Descrição |
|---|---|---|
| `CadastrarPecaInsumoUseCase` | `POST /pecas-insumos` | Cria nova peça com estoque inicial |
| `AlterarPecaInsumoUseCase` | `PUT /pecas-insumos/{id}` | Atualiza dados cadastrais da peça (imutabilidade via nova instância) |
| `ExcluirPecaInsumoUseCase` | `DELETE /pecas-insumos/{id}` | Remove peça do inventário |
| `BuscarPecaInsumoUseCase` | `GET /pecas-insumos/{id}` | Retorna dados de uma peça por ID |
| `ListarPecasInsumosUseCase` | `GET /pecas-insumos` | Lista todas as peças cadastradas |
| `ReservarPecaUseCase` | Interno (chamado por `ordemservico`) | Incrementa `quantidadeReservada`; valida estoque disponível |
| `ConsumirPecaUseCase` | Interno (chamado por `ordemservico`) | Decrementa `quantidadeEstoque` e `quantidadeReservada` |

### Métodos de Domínio em `PecaInsumo`

| Método | Descrição |
|---|---|
| `getQuantidadeDisponivel()` | `quantidadeEstoque - quantidadeReservada` — quantidade efetivamente disponível para novas reservas |
| Construtor sem `id` | Gera UUID via `UUID.randomUUID().toString()` para criação |
| Construtor com todos os campos | Para reconstituição a partir da persistência |

> `PecaInsumo` é imutável — todos os campos são `final`. Qualquer alteração cria uma nova
> instância com os valores atualizados.

### Regras de Negócio

1. **Estoque disponível para reserva** — `reservarPeca(qtd)` valida que
   `getQuantidadeDisponivel() >= qtd`; caso contrário lança `RegraDeNegocioException`.
2. **Quantidade de reserva para consumo** — `consumirPeca(qtd)` valida que
   `quantidadeReservada >= qtd`; impede consumo sem reserva prévia correspondente.
3. **Quantidade positiva** — `reservarPeca()` valida que `qtd > 0`.
4. **Imutabilidade** — `AlterarPecaInsumoService` cria uma nova instância de `PecaInsumo`
   preservando `id`, `quantidadeEstoque` e `quantidadeReservada` do objeto atual.

## Ciclo de Vida

```
[Inexistente]
      |
      | CadastrarPecaInsumo (POST /pecas-insumos)
      ▼
  [Cadastrado]
      |
      ├── AlterarPecaInsumo (PUT /pecas-insumos/{id})   [dados cadastrais]
      |
      ├── ReservarPeca (chamado por FinalizarDiagnostico / envio para orçamento)
      |         └── quantidadeReservada += qtd
      |
      ├── ConsumirPeca (chamado por FinalizarOrdemDeServico)
      |         └── quantidadeEstoque -= qtd
      |         └── quantidadeReservada -= qtd
      |
      └── ExcluirPecaInsumo (DELETE /pecas-insumos/{id})
```

## Dependências Internas

| Módulo | Direção | Detalhe |
|---|---|---|
| `ordemservico` | `ordemservico` consome `pecainsumo` | `FinalizarOrdemDeServicoService` chama `ConsumirPecaUseCase` para cada peça do orçamento |
| `orcamento` | `orcamento` referencia `pecainsumo` | `PecaOrcamento` armazena `pecaInsumoId` — referência por ID, sem FK formal no domínio |

## Dependências Externas

Nenhuma.

## Pontos de Entrada REST

Todos os endpoints exigem autenticação JWT.

---

### `POST /pecas-insumos` — Cadastrar peça

**Request body** (`CadastrarPecaInsumoRequest`):

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `nome` | `String` | Sim | Nome da peça ou insumo |
| `descricao` | `String` | Não | Descrição adicional |
| `precoUnitario` | `BigDecimal` | Sim | Preço de venda unitário |
| `quantidadeEstoque` | `Integer` | Sim | Quantidade inicial em estoque |
| `categoria` | `CategoriaPeca` | Sim | Categoria da peça |

**Respostas**:

| Status | Situação |
|---|---|
| 201 Created | Peça criada; `Location: /pecas-insumos/{id}` |
| 400 Bad Request | Dados inválidos ou categoria desconhecida |

---

### `GET /pecas-insumos` — Listar peças

**Respostas**:

| Status | Situação |
|---|---|
| 200 OK | Lista de `PecaInsumoResponse` |

---

### `GET /pecas-insumos/{id}` — Buscar peça por ID

**Path param**: `id` (UUID como String).

**Respostas**:

| Status | Situação |
|---|---|
| 200 OK | `PecaInsumoResponse` |
| 500 Internal Server Error | Peça não encontrada (`EntityNotFoundException` não tratada — ver CRI-001) |

---

### `PUT /pecas-insumos/{id}` — Alterar peça

**Request body** (`AlterarPecaInsumoRequest`): mesmo contrato do cadastro.

**Respostas**:

| Status | Situação |
|---|---|
| 204 No Content | Peça alterada |
| 404 Not Found | Peça não encontrada |

---

### `DELETE /pecas-insumos/{id}` — Excluir peça

**Respostas**:

| Status | Situação |
|---|---|
| 204 No Content | Peça excluída |
| 404 Not Found | Peça não encontrada |

## Modelos de Domínio

### `PecaInsumo` — Aggregate Root (POJO puro, sem JPA)

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `String` | UUID gerado na criação; imutável |
| `nome` | `String` | Nome da peça |
| `descricao` | `String` | Descrição opcional |
| `precoUnitario` | `BigDecimal` | Preço unitário de venda |
| `quantidadeEstoque` | `int` | Quantidade física em estoque |
| `quantidadeReservada` | `int` | Quantidade reservada para OS em andamento |
| `categoria` | `CategoriaPeca` | Categoria da peça |

**Calculado**: `getQuantidadeDisponivel()` = `quantidadeEstoque - quantidadeReservada`

### `CategoriaPeca` — Enum

| Valor | Descrição |
|---|---|
| `OLEO` | Óleos lubrificantes |
| `FILTRO` | Filtros de ar, óleo, combustível |
| `FREIO` | Componentes de freio |
| `SUSPENSAO` | Componentes de suspensão |
| `ELETRICO` | Componentes elétricos e eletrônicos |
| `MOTOR` | Componentes de motor |
| `OUTROS` | Demais categorias |

### `PecaInsumoJpaEntity` — Adapter de Persistência

Entidade JPA separada do domínio. Implementa o padrão de referência da constituição.

| Método | Descrição |
|---|---|
| `fromDomain(PecaInsumo)` | Static factory — cria `PecaInsumoJpaEntity` a partir do domínio |
| `toDomain()` | Converte `PecaInsumoJpaEntity` para `PecaInsumo` |

## Arquivos Críticos

| Arquivo | Responsabilidade |
|---|---|
| `pecainsumo/domain/model/PecaInsumo.java` | Aggregate root imutável; sem JPA; `getQuantidadeDisponivel()` |
| `pecainsumo/domain/model/CategoriaPeca.java` | Enum de categorias |
| `pecainsumo/domain/repository/PecaInsumoRepository.java` | Port (interface) de persistência |
| `pecainsumo/application/service/ReservarPecaService.java` | Valida disponibilidade e incrementa reserva |
| `pecainsumo/application/service/ConsumirPecaService.java` | Valida reserva e decrementa estoque + reserva |
| `pecainsumo/infrastructure/persistence/PecaInsumoJpaEntity.java` | Adapter JPA; `fromDomain()` / `toDomain()` — **padrão de referência** |
| `pecainsumo/infrastructure/web/PecaInsumoController.java` | Controller REST |

## Observações

- **Módulo de referência arquitetural**: a constituição (Princípio I) cita `pecainsumo` /
  `PecaInsumoJpaEntity` explicitamente como o padrão correto para separação entre domínio
  e persistência. Todos os demais módulos devem eventualmente seguir este padrão.
- **`quantidadeReservada` não zerada ao excluir peça**: se uma peça for excluída com
  `quantidadeReservada > 0`, a reserva é descartada sem notificação às OS que a referenciavam.
- **Desserialização de `CategoriaPeca`**: `ApiExceptionHandler` possui tratamento especial
  para `HttpMessageNotReadableException` com enum inválido — a mensagem de erro extrai o
  nome da enum dinamicamente para o payload de resposta.

---

## Pontos de Atenção

### Achados de Integridade Crítica

#### [CRI-001] `buscarPorId()` lança `EntityNotFoundException` não tratada pelo `ApiExceptionHandler`

**Componente afetado**: `PecaInsumoController#buscarPorId`

**Descrição**: `buscarPorId()` no controller lança `EntityNotFoundException` (Jakarta
Persistence) quando a peça não é encontrada. O `ApiExceptionHandler` não mapeia
`EntityNotFoundException` — o Spring retorna HTTP 500 com stack trace completo em vez de
HTTP 404.

**Impacto**: `GET /pecas-insumos/{id}` com ID inexistente sempre retorna HTTP 500, violando
o contrato REST e expondo detalhes de implementação no response body.

**Correção sugerida**: substituir `EntityNotFoundException` por `RecursoNaoEncontradoException`
(já definida e tratada no `ApiExceptionHandler`), ou adicionar o mapeamento de
`EntityNotFoundException → 404` no `ApiExceptionHandler`.

---

#### [CRI-002] `ReservarPecaService` e `ConsumirPecaService` usam exceção errada para "não encontrado"

**Componente afetado**: `ReservarPecaService#reservar` · `ConsumirPecaService#consumir`

**Descrição**: ambos os services lançam `RegraDeNegocioException` quando a peça não é
encontrada pelo ID (`pecaInsumoRepository.buscarPorId()` retorna `Optional.empty()`). A
constituição (Princípio X, implícito na convenção de handlers) define
`RecursoNaoEncontradoException` para entidades inexistentes — `RegraDeNegocioException` é
para violações de regras de negócio em entidades existentes.

**Impacto**: o chamador (`FinalizarOrdemDeServicoService`) recebe HTTP 400 ("Bad Request")
em vez de HTTP 404 ("Not Found") quando uma peça referenciada no orçamento não existe mais,
tornando o diagnóstico de erro ambíguo.

**Correção sugerida**: substituir `RegraDeNegocioException` por `RecursoNaoEncontradoException`
nos dois services quando a peça não for encontrada.

---

#### [CRI-003] Ausência de verificação de reservas ao excluir peça

**Componente afetado**: `ExcluirPecaInsumoService`

**Descrição**: uma peça pode ser excluída mesmo com `quantidadeReservada > 0` — ou seja,
referenciada em orçamentos de OS abertas. A exclusão quebra silenciosamente a referência:
`PecaOrcamento.pecaInsumoId` aponta para um ID que não existe mais. Quando
`FinalizarOrdemDeServicoService` chamar `ConsumirPecaUseCase` com esse ID, receberá
`RegraDeNegocioException` (ver CRI-002), retornando HTTP 400 e impedindo a finalização
da OS.

**Impacto**: OS com peças excluídas ficam presas — não podem ser finalizadas.

**Correção sugerida**: verificar `quantidadeReservada > 0` antes da exclusão e lançar
`RegraDeNegocioException` com mensagem informando que a peça está reservada em orçamentos
ativos.

---

### Achados de Melhoria

#### [MEL-001] Sem controle de estoque mínimo ou alerta de reposição

**Componente afetado**: `PecaInsumo` · `ListarPecasInsumosUseCase`

**Descrição**: não há campo de estoque mínimo nem mecanismo de alerta quando o estoque
de uma peça cai abaixo de um limiar. A listagem não distingue peças com baixo estoque.

**Sugestão de backlog**: adicionar campo `estoqueMinimo` em `PecaInsumo` e filtro `GET
/pecas-insumos?estoqueAbaixoDoMinimo=true` para facilitar reposição.

---

#### [MEL-002] Ausência de paginação no endpoint de listagem

**Componente afetado**: `GET /pecas-insumos` · `ListarPecasInsumosUseCase`

**Descrição**: a listagem retorna todas as peças sem paginação ou filtros. Com inventário
grande, a resposta pode ser excessivamente grande.

**Sugestão de backlog**: adicionar `Pageable` e filtros por `categoria` e `nome` (busca
parcial) ao endpoint de listagem.

---

#### [MEL-003] Ausência de histórico de movimentações de estoque

**Componente afetado**: módulo completo `pecainsumo`

**Descrição**: não há rastreabilidade das operações de reserva e consumo — quem reservou,
quando, para qual OS. A quantidade em estoque pode estar incorreta por bug sem como auditar.

**Sugestão de backlog**: criar entidade `MovimentacaoEstoque` com os campos
`pecaInsumoId`, `tipo` (`RESERVA`, `CONSUMO`, `AJUSTE`), `quantidade`, `referenciaOS` e
`ocorridoEm`.
