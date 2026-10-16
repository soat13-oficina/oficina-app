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
| `BuscarPecaInsumoPorIdUseCase` | `GET /pecas-insumos/{id}` | Retorna dados de uma peça por ID |
| `ListarPecasInsumosUseCase` | `GET /pecas-insumos` | Lista todas as peças cadastradas |
| `AdicionarEstoquePecaUseCase` / `RemoverEstoquePecaUseCase` | `POST /pecas-insumos/{id}/adicionar-estoque` · `/remover-estoque` | Entrada e saída manual de estoque |
| `ReservarPecaUseCase` / `LiberarReservaPecaUseCase` / `ConsumirPecaUseCase` | `POST /pecas-insumos/{id}/reservar` · `/liberar-reserva` · `/consumir` | Ajuste manual do contador de uma peça, sem dono. **O fluxo da OS não usa mais** — usa as reservas por OS abaixo (Fase 4) |
| `ConsumirReservasDaOrdemUseCase` | Interno (conclusão do serviço, via `br.com.oficina.ponte.ReservasDaOrdemNoEstoque`) | Baixa do estoque as peças reservadas para a OS e marca as reservas `CONSUMIDA` — Fase 4 |

### Participante da Saga (Fase 4, futura Execution Service)

Comandos recebidos em `execution-commands` (`infrastructure/mensageria/ManipuladorComandosEstoque`),
respostas em `os-saga-replies` com `causationId` = o comando (`application/saga/ReservasPelaSaga`).

| Comando | Efeito | Resposta |
|---|---|---|
| `ReservarPecas` | **Tudo ou nada**: lê as peças com trava pessimista, em ordem de id; com qualquer peça faltando (ou fora do cadastro) não reserva nada. Grava uma linha por peça em `reservas_pecas` (V24) e soma em `quantidadeReservada` | `PecasReservadas` ou `ReservaFalhou` com as faltantes (solicitada × disponível) |
| `LiberarReserva` | Devolve ao disponível o que está reservado para a OS e marca as reservas `LIBERADA` | `ReservaLiberada` (lista vazia se não havia reserva) |

Idempotentes pelo estado: `ReservarPecas` reenviado encontra as reservas ativas da OS e só repete a
resposta; `LiberarReserva` repetido confirma com lista vazia.

### Métodos de Domínio em `PecaInsumo`

| Método | Descrição |
|---|---|
| `getQuantidadeDisponivel()` | `quantidadeEstoque - quantidadeReservada` — quantidade efetivamente disponível para novas reservas |
| `reservar(qtd)` / `liberarReserva(qtd)` / `consumirReservada(qtd)` | Usados pela Saga e pela conclusão; devolvem nova instância e nunca deixam contador negativo |
| Construtor sem `id` | Gera UUID via `UUID.randomUUID().toString()` para criação |
| Construtor com todos os campos | Para reconstituição a partir da persistência |

> `PecaInsumo` é imutável — todos os campos são `final`. Qualquer alteração cria uma nova
> instância com os valores atualizados.

`ReservaPeca` (record, Fase 4): quanto de uma peça está reservado para uma OS — `RESERVADA`,
`CONSUMIDA` ou `LIBERADA`. Chave: OS + peça. Na Execution Service vira a tabela
`oficina-<env>-reservas` do DynamoDB (PK `ordemServicoId`, SK `pecaId`).

### Regras de Negócio

1. **Estoque disponível para reserva** — reservar exige `getQuantidadeDisponivel() >= qtd`.
2. **Quantidade de reserva para consumo** — o consumo manual (`consumirPeca`) exige
   `quantidadeReservada >= qtd`.
3. **Quantidade positiva** — reservas exigem `qtd > 0`.
4. **Imutabilidade** — `AlterarPecaInsumoService` cria uma nova instância de `PecaInsumo`
   preservando `id`, `quantidadeEstoque` e `quantidadeReservada` do objeto atual.
5. **Reserva tem dono** (Fase 4) — toda reserva feita pela Saga fica em `reservas_pecas` com a OS;
   é o que permite liberar "a reserva desta OS" e baixar só o que ela reservou na conclusão.

## Ciclo de Vida

```
[Inexistente]
      |
      | CadastrarPecaInsumo (POST /pecas-insumos)
      ▼
  [Cadastrado]
      |
      ├── AlterarPecaInsumo (PUT /pecas-insumos/{id})        [dados cadastrais]
      |
      ├── ReservarPecas (Saga, depois da aprovação)           quantidadeReservada += qtd; reserva RESERVADA
      |
      ├── LiberarReserva (Saga: pagamento recusado/expirado,  quantidadeReservada -= qtd; reserva LIBERADA
      |                  execução abortada, reserva sem resposta)
      |
      ├── Conclusão do serviço (ConsumirReservasDaOrdem)      quantidadeEstoque -= qtd; quantidadeReservada -= qtd;
      |                                                       reserva CONSUMIDA
      |
      └── ExcluirPecaInsumo (DELETE /pecas-insumos/{id})
```

## Dependências Internas

| Módulo | Direção | Detalhe |
|---|---|---|
| `ordemservico` (Saga) | só mensagens | Consome `ReservarPecas` e `LiberarReserva`; publica `PecasReservadas`, `ReservaFalhou` e `ReservaLiberada` |
| `ordemservico` | porta `ReservasDaOrdem` | A conclusão do serviço chama `ConsumirReservasDaOrdemUseCase` pela ponte |
| `ordemservico` | porta `CatalogoPecas` | Existência das peças do diagnóstico, pela ponte `CatalogoDePecasDoEstoque` |

## Dependências Externas

| Serviço | Como | Detalhe |
|---|---|---|
| `oficina-billing-service` | `POST /internal/pecas-insumos/consulta` (`X-Service-Token`) | Preço e descrição das peças para gerar o orçamento |

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
| 404 Not Found | Peça não encontrada |

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
| 400 Bad Request | Peça possui reserva ativa e não pode ser excluída |
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
| `pecainsumo/application/saga/ReservasPelaSaga.java` | `ReservarPecas` (tudo ou nada, com trava) e `LiberarReserva` — Fase 4 |
| `pecainsumo/application/service/ConsumirReservasDaOrdemService.java` | Baixa as reservas da OS na conclusão do serviço — Fase 4 |
| `pecainsumo/infrastructure/persistence/ReservaPecaJpaEntity.java` | Tabela `reservas_pecas` (V24), chave OS + peça |
| `pecainsumo/infrastructure/persistence/PecaInsumoJpaEntity.java` | Adapter JPA; `fromDomain()` / `toDomain()` — **padrão de referência** |
| `pecainsumo/infrastructure/web/PecaInsumoController.java` | Controller REST |

## Observações

- **Módulo de referência arquitetural**: a constituição (Princípio I) cita `pecainsumo` /
  `PecaInsumoJpaEntity` explicitamente como o padrão correto para separação entre domínio
  e persistência. Todos os demais módulos devem eventualmente seguir este padrão.
- **Excluir peça reservada**: a FK de `reservas_pecas` para `pecas_insumos` impede excluir uma
  peça que tenha reserva registrada (Fase 4). Peças só com o contador manual
  (`quantidadeReservada > 0` sem linha em `reservas_pecas`) ainda podem ser excluídas.
- **Desserialização de `CategoriaPeca`**: `ApiExceptionHandler` possui tratamento especial
  para `HttpMessageNotReadableException` com enum inválido — a mensagem de erro extrai o
  nome da enum dinamicamente para o payload de resposta.

---

## Pontos de Atenção

### Achados de Integridade Crítica

> Status: resolvidos pela spec `004-pecainsumo-achados-criticos`. Esta seção permanece como
> registro histórico dos bloqueantes encontrados e da correção aplicada.

#### [CRI-001] `buscarPorId()` lançava `EntityNotFoundException` não tratada pelo `ApiExceptionHandler`

**Componente afetado**: `PecaInsumoController#buscarPorId`

**Descrição**: `buscarPorId()` no controller lançava `EntityNotFoundException` (Jakarta
Persistence) quando a peça não é encontrada. O `ApiExceptionHandler` não mapeia
`EntityNotFoundException` — o Spring retornava HTTP 500 com stack trace completo em vez de
HTTP 404.

**Impacto**: `GET /pecas-insumos/{id}` com ID inexistente retornava HTTP 500, violando
o contrato REST e expondo detalhes de implementação no response body.

**Correção aplicada**: `PecaInsumoController#buscarPorId` passou a lançar
`RecursoNaoEncontradoException`, já tratada pelo `ApiExceptionHandler` como HTTP 404 no
formato padronizado `ErrorResponse(timestamp, status, message)`.

---

#### [CRI-002] `ReservarPecaService` e `ConsumirPecaService` usavam exceção errada para "não encontrado"

**Componente afetado**: `ReservarPecaService#reservar` · `ConsumirPecaService#consumir`

**Descrição**: ambos os services lançavam `RegraDeNegocioException` quando a peça não era
encontrada pelo ID (`pecaInsumoRepository.buscarPorId()` retorna `Optional.empty()`). A
constituição (Princípio X, implícito na convenção de handlers) define
`RecursoNaoEncontradoException` para entidades inexistentes — `RegraDeNegocioException` é
para violações de regras de negócio em entidades existentes.

**Impacto**: o chamador (`FinalizarOrdemDeServicoService`) recebia HTTP 400 ("Bad Request")
em vez de HTTP 404 ("Not Found") quando uma peça referenciada no orçamento não existe mais,
tornando o diagnóstico de erro ambíguo.

**Correção aplicada**: `ReservarPecaService` e `ConsumirPecaService` passaram a lançar
`RecursoNaoEncontradoException` somente quando a peça não existe. As validações de quantidade,
estoque e reserva insuficiente continuam usando `RegraDeNegocioException` e HTTP 400.

---

#### [CRI-003] Ausência de verificação de reservas ao excluir peça

**Componente afetado**: `ExcluirPecaInsumoService`

**Descrição**: uma peça podia ser excluída mesmo com `quantidadeReservada > 0` — ou seja,
referenciada em orçamentos de OS abertas. A exclusão quebra silenciosamente a referência:
`PecaOrcamento.pecaInsumoId` aponta para um ID que não existe mais. Quando
`FinalizarOrdemDeServicoService` chamava `ConsumirPecaUseCase` com esse ID, recebia erro
de "peça não encontrada" e a OS não podia ser finalizada.

**Impacto**: OS com peças excluídas ficavam presas — não podiam ser finalizadas.

**Correção aplicada**: `ExcluirPecaInsumoService` busca a peça antes de excluir; se o ID
não existe, lança `RecursoNaoEncontradoException` (HTTP 404); se `quantidadeReservada > 0`,
lança `RegraDeNegocioException` (HTTP 400) e preserva a peça; caso contrário, remove a peça
e registra log de sucesso.

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
