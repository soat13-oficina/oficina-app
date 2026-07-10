# Domínio: Pessoa Global

## Objetivo

Manter um cadastro único de pessoas físicas **por tenant**, reutilizável entre os contextos de
`ContaUsuario`, `Paciente` e `Profissional` dentro do mesmo tenant — evitando triplicação de
dados pessoais básicos para a mesma pessoa em papéis diferentes.

## Responsabilidade Principal

Prover uma identidade pessoal única por tenant (CPF único por tenant) que serve de elo entre
os domínios de identidade, pacientes e profissionais. O ciclo de vida de `PessoaGlobal` é
governado pelos use cases `DesativarPessoaGlobalUseCase` e `ReativarPessoaGlobalUseCase` deste
domínio; a criação permanece nos domínios consumidores (`tenant`, `pacientes`, `profissionais`).

## Funcionalidades Implementadas

### Use Cases

| Use Case | Restrição | Descrição |
|----------|-----------|-----------|
| `DesativarPessoaGlobalUseCase` | ADMINISTRADOR | Desativa em cascata: PessoaGlobal → ContaUsuario(s) → Profissional → Paciente(s), na mesma transação. Pula entidades já inativas. Rejeita pessoa anonimizada por LGPD. |
| `ReativarPessoaGlobalUseCase` | ADMINISTRADOR | Reativa em cascata as mesmas entidades. Rejeita pessoa anonimizada por LGPD (exclusão irreversível). |

### Métodos de Domínio em `PessoaGlobal`

| Método | Descrição |
|--------|-----------|
| `desativar()` | Define `ativo=false`. |
| `reativar()` | Define `ativo=true`; lança `IllegalArgumentException` se a pessoa foi anonimizada por LGPD. |
| `anonimizarParaLgpd()` | Substitui `nomeCompleto` por `"[EXCLUÍDO]"`, esvazia `contatos`, define `ativo=false`. Irreversível. |
| `atualizarContatos(List<Contato>)` | Atualiza contatos normalizando principal único por `TipoContato` (ver regras abaixo). |
| `foiAnonimizadaPorLgpd()` | Retorna `true` se `nomeCompleto == "[EXCLUÍDO]"`. |

### Normalização de Contatos (FR-010 / FR-011)

- Se múltiplos contatos do mesmo tipo entram com `principal=true`, apenas o **primeiro** é mantido como principal; os demais são desmarcados.
- Se há **exatamente um** contato de um tipo e ele vem com `principal=false`, é automaticamente promovido a `true`.
- Múltiplos contatos do mesmo tipo **sem** nenhum principal: permitido, sem erro.

## Ciclo de Vida de `PessoaGlobal.ativo`

```
ATIVA  ──DesativarPessoaGlobalUseCase──▶  INATIVA
INATIVA ──ReativarPessoaGlobalUseCase──▶  ATIVA
ATIVA/INATIVA ──ProcessarExclusaoDadosUseCase (ciclovida)──▶  ANONIMIZADA_IRREVERSIVEL
```

A desativação/reativação administrativa é distinta e reversível. A exclusão LGPD é definitiva.

## Dependências Internas

- `shared/domain/EntidadeBase` — `PessoaGlobal` estende `EntidadeBase` e possui `tenantId`
- `identidade` — `ContaUsuario` referencia `pessoaGlobalId`; `DesativarPessoaGlobalUseCase` acessa `ContaUsuarioRepository`
- `pacientes` — `Paciente` referencia `pessoaGlobalId`; use cases acessam `PacienteRepository`
- `profissionais` — `Profissional` referencia `pessoaGlobalId`; use cases acessam `ProfissionalRepository`
- `tenant` — `CriarTenantEClinicaUseCase` cria `PessoaGlobal` para o administrador inicial
- `ciclovida` — `ProcessarExclusaoDadosUseCase` chama `PessoaGlobal.anonimizarParaLgpd()` durante exclusão LGPD
- `shared/infrastructure` — `ProfissionalContexto.resolverAtivo()` valida `pessoaGlobal.tenantId` na cadeia `usuarioId → pessoaGlobalId → profissionalId`

## Dependências Externas

Nenhuma.

## Pontos de Entrada (REST)

Nenhum endpoint REST — domínio sem interface pública. Os use cases são contratos internos
invocados pelos domínios consumidores.

## Modelos de Domínio

- **`PessoaGlobal`**: `id` (UUID, PK), `tenantId` (UUID, FK para `tenants`), `nomeCompleto` (String, PII), `cpf` (String 11 dígitos, único por tenant — PII), `dataNascimento` (LocalDate, PII), `contatos` (List\<Contato\>, JSONB — PII), `ativo` (Boolean, gerenciado pelos use cases deste domínio)
- **`Contato`**: `tipo` (EMAIL | WHATSAPP | TELEFONE), `valor` (String), `principal` (Boolean — no máximo um `true` por tipo)

**Invariantes**:
- `cpf` com exatamente 11 dígitos numéricos; `nomeCompleto` não pode ser vazio.
- `PessoaGlobal.cpf` e `tenantId` são imutáveis após criação.
- Exclusão LGPD é irreversível; reativação administrativa não se aplica a pessoas anonimizadas.

**Constraint de banco**: `UNIQUE (tenant_id, cpf)` — uma pessoa por CPF por tenant.

## Arquivos Críticos

- `PessoaGlobal.kt` — modelo com validação de CPF no `init` e métodos de ciclo de vida
- `PessoaGlobalRepository.kt` — `findByTenantIdAndCpf`, `existsByTenantIdAndCpf`, `findByTenantIdAndId`
- `DesativarPessoaGlobalUseCase.kt` — cascata administrativa de desativação
- `ReativarPessoaGlobalUseCase.kt` — cascata administrativa de reativação

## Observações

- **Dados PII sensíveis**: `nomeCompleto`, `cpf`, `dataNascimento` e `contatos` são PII; logs e métricas DEVEM excluir estes campos (Constituição IV).
- **`PessoaGlobal` é por-tenant, não global entre tenants**: o nome "Global" reflete reutilização entre papéis (`Paciente`, `Profissional`, `ContaUsuario`) dentro do mesmo tenant.
- **Gate clínico externo ao domínio**: `ProfissionalContexto.resolverAtivo()` usa `Profissional.ativo` como gate único; não verifica `PessoaGlobal.ativo`. A garantia de acesso zero após desativação vem da cascata `DesativarPessoaGlobalUseCase → Profissional.ativo=false` (decisão registrada na feature 006).
- **Acoplamento implícito de criação/reuso**: a lógica "criar ou reutilizar `PessoaGlobal`" permanece dispersa nos domínios `tenant`, `pacientes` e `profissionais`. Não há `CriarPessoaGlobalUseCase` neste domínio — decisão intencional para evitar crescimento prematuro de contrato.

## Débitos em Aberto

Nenhum débito técnico aberto neste domínio após a feature 007.

## Planejado em Spec Kit

- `specs/007-debitos-pessoaglobal/` — feature que implementou o ciclo de vida completo, exclusão LGPD, normalização de contatos e cascata administrativa.
