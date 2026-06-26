# Domínio: Cliente

## Objetivo

Manter o cadastro de clientes da oficina (pessoas físicas e jurídicas), provendo busca por
nome e documento. É o ponto de entrada de identidade que os domínios `veiculo` e
`ordemservico` consultam para validar existência e obter dados do proprietário.

## Responsabilidade Principal

Gerenciar o ciclo de vida de clientes com validação de documento (CPF para PF, CNPJ para PJ),
unicidade de CPF/CNPJ e pesquisa flexível por nome ou documento com normalização de texto.

## Funcionalidades Implementadas

### Use Cases

| Use Case | Endpoint | Restrições |
|---|---|---|
| `CadastrarClienteUseCase` | `POST /clientes` | CPF/CNPJ único; documento e tipo devem ser informados juntos; CPF deve ter 11 dígitos, CNPJ 14 |
| `AlterarClienteUseCase` | `PUT /clientes/{clienteId}` | Cliente deve existir; CPF/CNPJ único exceto para o próprio cliente |
| `ExcluirClienteUseCase` | `DELETE /clientes/{clienteId}` | Cliente deve existir; remoção física permanente |
| `ConsultarClienteUseCase` | `GET /clientes/{clienteId}` | Retorna 404 se não encontrado |
| `PesquisarClientesUseCase` | `GET /clientes` | Busca por nome (contém, case-insensitive, ignora acentos) ou por CPF/CNPJ exato; sem termo retorna todos ordenados por nome |

### Métodos de Domínio em `Cliente`

| Método | Descrição |
|---|---|
| `alterar(nome, cpfOuCnpj, tipoCliente, email)` | Atualiza todos os campos mutáveis; re-executa as validações de nome e documento |
| `reconstituir(id, ...)` | Factory method estático para recriar a entidade a partir da persistência |

### Regras de Negócio

1. **Nome obrigatório** — não pode ser nulo nem em branco.
2. **Consistência tipo/documento** — se `tipoCliente` for informado sem `cpfOuCnpj`, ou vice-versa, a operação é rejeitada.
3. **Quantidade de dígitos** — PF: exatamente 11 dígitos numéricos; PJ: exatamente 14 dígitos numéricos (apenas dígitos são contados — formatação como pontos e traços é ignorada).
4. **Unicidade de CPF/CNPJ** — apenas um cliente pode ter o mesmo número de documento no sistema.
5. **Email opcional** — normalizado (trim) antes de persistir; `null` se em branco.

## Ciclo de Vida

```
[Inexistente]
      |
      | CadastrarCliente (POST /clientes)
      ▼
  [Cadastrado] ◄──── AlterarCliente (PUT /clientes/{id})
      |
      | ExcluirCliente (DELETE /clientes/{id})
      ▼
  [Removido — permanente]
```

## Dependências Internas

| Módulo | Direção | Detalhe |
|---|---|---|
| `veiculo` | `veiculo` consome `cliente` | `CadastrarVeiculoService` chama `ClienteRepository.buscarPorId()` para validar o proprietário |
| `ordemservico` | `ordemservico` consome `cliente` | `CriarNovaOrdemDeServicoService` chama `ClienteRepository.buscarPorId()` para obter dados do cliente |
| `notificacao` | `notificacao` consome `cliente` | `EnviarNotificacaoStatusOSService` chama `ClienteRepository.buscarPorId()` para obter o email do cliente |

> Ao criar uma `OrdemDeServico`, os dados do cliente (nome, documento, tipo) são capturados
> como **snapshot** denormalizado na OS. Alterações futuras no cadastro do cliente não
> retroagem às OS já criadas.

## Dependências Externas

Nenhuma.

## Pontos de Entrada REST

Todos os endpoints exigem autenticação JWT.

---

### `POST /clientes` — Cadastrar cliente

**Request body** (`CadastrarClienteRequest`):

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `nome` | `String` | Sim | Nome do cliente |
| `cpfOuCnpj` | `String` | Condicional | CPF (11 dígitos) ou CNPJ (14 dígitos); obrigatório se `tipoCliente` for informado |
| `tipoCliente` | `TipoCliente` | Condicional | `PF` ou `PJ`; obrigatório se `cpfOuCnpj` for informado |
| `email` | `String` | Não | Email para notificações |

**Respostas**:

| Status | Situação |
|---|---|
| 201 Created | Cliente criado; `Location: /clientes/{id}` |
| 400 Bad Request | Dados inválidos, documento inválido ou CPF/CNPJ já cadastrado |

---

### `GET /clientes` — Pesquisar clientes

**Query params**:

| Param | Tipo | Descrição |
|---|---|---|
| `termo` | `String` | Opcional. Busca por nome (contém, sem acento, case-insensitive) ou CPF/CNPJ exato |

**Respostas**:

| Status | Situação |
|---|---|
| 200 OK | Lista de clientes ordenada por nome; vazia se nenhum resultado |

---

### `GET /clientes/{clienteId}` — Consultar cliente por ID

**Path param**: `clienteId` (UUID).

**Respostas**:

| Status | Situação |
|---|---|
| 200 OK | `ClienteResponse` |
| 400 Bad Request | `clienteId` não é UUID válido |
| 404 Not Found | Cliente não encontrado |

---

### `PUT /clientes/{clienteId}` — Alterar cliente

**Request body** (`AlterarClienteRequest`): mesmo contrato do cadastro, sem `clienteId`.

**Respostas**:

| Status | Situação |
|---|---|
| 204 No Content | Cliente alterado |
| 400 Bad Request | Dados inválidos ou CPF/CNPJ já cadastrado para outro cliente |
| 404 Not Found | Cliente não encontrado |

---

### `DELETE /clientes/{clienteId}` — Excluir cliente

**Respostas**:

| Status | Situação |
|---|---|
| 204 No Content | Cliente excluído |
| 400 Bad Request | `clienteId` não é UUID válido |
| 404 Not Found | Cliente não encontrado |

---

## Modelos de Domínio

### `Cliente` — Aggregate Root

| Campo | Tipo Java | Coluna DB | Constraint | Descrição |
|---|---|---|---|---|
| `id` | `UUID` | `id` | PK, `NOT NULL`, gerado via `GenerationType.UUID` | Identificador do cliente |
| `nome` | `String` | `nome` | `NOT NULL` | Nome completo ou razão social |
| `cpfOuCnpj` | `String` | `cpf_ou_cnpj` | Opcional | CPF ou CNPJ (sem formatação) |
| `tipoCliente` | `TipoCliente` | `tipo_cliente` | Opcional, `EnumType.STRING` | `PF` ou `PJ` |
| `email` | `String` | `email` | Opcional | Email para notificações |

### `TipoCliente` — Enum

| Valor | Descrição |
|---|---|
| `PF` | Pessoa Física — CPF com 11 dígitos numéricos |
| `PJ` | Pessoa Jurídica — CNPJ com 14 dígitos numéricos |

### `ClienteResponse` — Representação de Saída

```json
{
  "id": "uuid",
  "nome": "Maria Silva",
  "cpfOuCnpj": "12345678901",
  "tipoCliente": "PF",
  "email": "maria@email.com"
}
```

## Arquivos Críticos

| Arquivo | Responsabilidade |
|---|---|
| `cliente/domain/model/Cliente.java` | Aggregate root; validação de nome e documento; método `alterar()` |
| `cliente/domain/model/TipoCliente.java` | Enum PF / PJ |
| `cliente/domain/repository/ClienteRepository.java` | Port (interface) de persistência |
| `cliente/application/service/CadastrarClienteService.java` | Valida unicidade de CPF/CNPJ antes de persistir |
| `cliente/application/service/AlterarClienteService.java` | Valida unicidade de CPF/CNPJ exceto para o próprio cliente |
| `cliente/application/service/ExcluirClienteService.java` | Remoção física por ID |
| `cliente/application/service/PesquisarClientesService.java` | Busca em memória com normalização de texto e ordenação por nome |
| `cliente/infrastructure/persistence/JpaClienteRepository.java` | Adapter JPA; busca por documento filtra em memória |

## Observações

- **Validação de dígitos, não de dígito verificador**: o domínio valida apenas a quantidade
  de dígitos do CPF/CNPJ, não o dígito verificador. Um CPF como `11111111111` (11 dígitos)
  é aceito mesmo sendo matematicamente inválido.
- **Busca de documento em memória**: `buscarPorDocumento()` e `buscarPorNomeEDocumento()`
  no `JpaClienteRepository` carregam todos os registros e filtram via stream. Isso é
  dívida técnica — com volume de dados, causa degradação de performance.
- **Pesquisa normalizada no service**: `PesquisarClientesService` normaliza o termo (remove
  acentos, lowercase) para busca por nome, e mantém somente dígitos para comparação de
  documento. A pesquisa retorna todos os clientes ordenados por nome quando o termo é vazio.

---

## Pontos de Atenção

### Achados de Integridade Crítica

#### [CRI-001] Hard delete sem verificação de integridade referencial

**Componente afetado**: `ExcluirClienteService` · `JpaClienteRepository#excluirPorId`

**Descrição**: `ExcluirClienteService` remove o cliente fisicamente sem verificar se existem
veículos ou ordens de serviço vinculados. As FKs `fk_veiculos_cliente_id`
(`veiculos.cliente_id → clientes.id`) e `fk_ordens_de_servico_cliente_id`
(`ordens_de_servico.cliente_id → clientes.id`) causam `DataIntegrityViolationException`
quando o cliente possui registros associados. O `ApiExceptionHandler` não mapeia essa
exceção, resultando em HTTP 500.

**Impacto**: a exclusão de qualquer cliente ativo — com veículos ou OS — retorna HTTP 500
sem mensagem explicativa. Qualquer oficina em operação terá clientes com histórico.

**Correção sugerida**: verificar a existência de veículos e OS vinculados antes de remover
o cliente, lançando `RegraDeNegocioException` com mensagem descritiva.

---

#### [CRI-002] Busca de unicidade de documento em memória

**Componente afetado**: `JpaClienteRepository#buscarPorDocumento`

**Descrição**: a validação de unicidade de CPF/CNPJ nos serviços `CadastrarClienteService`
e `AlterarClienteService` chama `buscarPorDocumento()`, que executa `repository.findAll()`
e filtra via stream em memória. Com crescimento da base de clientes, essa operação degrada
linearmente e pode ser executada em operações concorrentes de cadastro, criando uma condição
de corrida (race condition) para unicidade — dois cadastros simultâneos com o mesmo
documento passam na verificação antes que qualquer um persista.

**Impacto**: violação silenciosa de unicidade em cenário de concorrência; degradação de
performance com volume de dados.

**Correção sugerida**: adicionar índice `UNIQUE` em `clientes.cpf_ou_cnpj` via migração
Flyway e delegar a verificação ao banco, tratando a `DataIntegrityViolationException`
resultante como `RegraDeNegocioException`.

---

### Achados de Melhoria

#### [MEL-001] Anotações JPA no aggregate root — dívida técnica arquitetural

**Componente afetado**: `Cliente.java`

**Descrição**: `Cliente` possui `@Entity`, `@Table`, `@Column` diretamente no aggregate
root, violando o Princípio I da constituição. A referência correta é `PecaInsumo` com
`PecaInsumoJpaEntity` separada.

**Sugestão de backlog**: criar `ClienteJpaEntity` separada com `fromDomain()` / `toDomain()`.

---

#### [MEL-002] Sem validação de formato real de CPF/CNPJ

**Componente afetado**: `Cliente#validarDocumento`

**Descrição**: apenas a quantidade de dígitos é verificada — o dígito verificador não é
validado. Documentos como `11111111111` (CPF) são aceitos.

**Sugestão de backlog**: adicionar validação de dígito verificador via algoritmo de módulo
11 ou biblioteca dedicada (ex.: `caelum-stella`).

---

#### [MEL-003] Ausência de paginação e filtro delegado ao banco no endpoint de pesquisa

**Componente afetado**: `PesquisarClientesService` · `GET /clientes`

**Descrição**: a pesquisa carrega todos os clientes em memória e filtra por stream, sem
paginação. Consultas sem termo retornam toda a tabela.

**Sugestão de backlog**: migrar a busca para query JPA/Specification com paginação via
`Pageable`. Avaliar indexação de `nome` e `cpf_ou_cnpj` para performance.

---

#### [MEL-004] Ausência de logging no `PesquisarClientesService`

**Componente afetado**: `PesquisarClientesService`

**Descrição**: `PesquisarClientesService` é o único service do domínio sem logging SLF4J,
violando o Princípio V da constituição.

**Sugestão de backlog**: adicionar `log.info()` nas operações de pesquisa, conforme o
padrão dos demais services do módulo.
