# Domínio: Auth

## Objetivo

Prover autenticação stateless via JWT para todos os endpoints protegidos da API, além de
gerenciar o registro e login de usuários do sistema. Protege também o endpoint de webhook
de decisão de orçamento com um token compartilhado (`X-Webhook-Token`).

## Responsabilidade Principal

Emitir tokens JWT após autenticação por credenciais (email + senha), validar tokens em cada
requisição via filtro e autorizar as rotas da aplicação. É o único ponto de criação e
validação de usuários com acesso ao sistema.

## Funcionalidades Implementadas

### Use Cases / Operações

| Operação | Endpoint | Descrição |
|---|---|---|
| Login | `POST /api/auth/login` | Autentica com email e senha; retorna JWT válido por 24h |
| Registro | `POST /api/auth/register` | Cria novo usuário; retorna JWT imediatamente após criação |
| Validação JWT | Filtro automático | Valida o Bearer token em cada requisição para rotas protegidas |
| Gerar token de integração | `POST /integracoes/tokens` | (JWT) Emite um token opaco para o webhook; segredo exibido **uma única vez**, armazenado só como hash |
| Listar tokens de integração | `GET /integracoes/tokens` | (JWT) Lista tokens (rótulo, status, autoria); nunca expõe o segredo |
| Revogar token de integração | `DELETE /integracoes/tokens/{id}` | (JWT) Revoga um token; deixa de ser aceito no webhook |
| Validação Webhook | Filtro automático | Valida o header `X-Webhook-Token` no endpoint de decisão (token de integração gerado **ou** segredo estático na transição) |

### Mecanismos de Segurança

| Componente | Responsabilidade |
|---|---|
| `JwtAuthenticationFilter` | Extrai e valida o Bearer token do header `Authorization`; autentica o contexto Spring Security |
| `WebhookTokenFilter` | Valida o header `X-Webhook-Token` no path `POST /integracoes/orcamentos/*/decisao`; aceita um **token de integração gerado ATIVO** (lookup por hash) **ou** o segredo estático (coexistência temporária); retorna 401 se ausente/inválido/revogado |
| `TokenIntegracaoService` | Gera (segredo via `SecureRandom`, hash SHA-256), valida (lookup por hash) e revoga tokens de integração; persiste apenas o hash + autoria |
| `JwtUtil` | Gera e valida tokens JWT usando HMAC-SHA; o subject do token é o email do usuário |
| `UserDetailsServiceImpl` | Carrega `UserDetails` a partir do email para validação pelo Spring Security |
| `SecurityConfig` | Define a cadeia de filtros, política stateless e lista de rotas públicas |
| `BCryptPasswordEncoder` | Hashing de senhas com BCrypt |

### Rotas Públicas (sem autenticação)

| Path | Motivo |
|---|---|
| `/api/auth/**` | Login e registro de usuário |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Documentação OpenAPI |
| `POST /integracoes/orcamentos/*/decisao` | Webhook de decisão; protegido por token dedicado (`X-Webhook-Token`), não por JWT |

## Ciclo de Vida

```
[Novo usuário]
      |
      | POST /api/auth/register
      ▼
  [Cadastrado] ──── POST /api/auth/login ──── [Token JWT emitido]
                                                      |
                                          [Cada requisição autenticada]
                                                      |
                                       JwtAuthenticationFilter valida token
                                                      |
                                          [Autorizado] ou [401/403]
```

O token expira após `jwt.expiration` milissegundos (padrão: 86400000 ms = 24h). Não há
refresh token — expirado, o usuário deve autenticar novamente.

## Dependências Internas

Nenhum outro módulo de negócio é consumido pelo `auth`. Os demais módulos dependem
indiretamente de `auth` por estarem protegidos pela `SecurityFilterChain`.

## Dependências Externas

- **Spring Security** — `SecurityFilterChain`, `AuthenticationManager`, `BCryptPasswordEncoder`
- **jjwt 0.12.6** — geração e validação de tokens JWT (`Jwts.builder()`, `Jwts.parser()`)

## Pontos de Entrada REST

### `POST /api/auth/login` — Login

**Request body** (`LoginRequest`):

| Campo | Tipo | Descrição |
|---|---|---|
| `email` | `String` | Email do usuário |
| `senha` | `String` | Senha em texto plano |

**Respostas**:

| Status | Situação |
|---|---|
| 200 OK | `{ "token": "<jwt>" }` |
| 401 Unauthorized | Credenciais inválidas |

---

### `POST /api/auth/register` — Registro

**Request body** (`LoginRequest`): mesmo formato do login.

**Respostas**:

| Status | Situação |
|---|---|
| 200 OK | `{ "token": "<jwt>" }` — usuário criado e token emitido |
| 409 Conflict | Email já cadastrado |
| 500 Internal Server Error | Erro genérico não tratado |

## Modelos de Domínio

### `Usuario` — Entidade

| Campo | Tipo | Constraint | Descrição |
|---|---|---|---|
| `id` | `Long` | PK, `BIGSERIAL` | Identificador gerado por auto-incremento |
| `email` | `String` | `NOT NULL`, `UNIQUE` | Email do usuário (subject do JWT) |
| `senha` | `String` | `NOT NULL` | Senha armazenada com hash BCrypt |

> **Exceção de padrão**: `auth.Usuario` usa `Long`/BIGSERIAL como ID — diferente dos demais
> módulos de negócio que usam UUID. Essa exceção é explicitamente registrada na constituição.

## Arquivos Críticos

| Arquivo | Responsabilidade |
|---|---|
| `auth/infrastructure/security/SecurityConfig.java` | Define `SecurityFilterChain`, política stateless, rotas públicas e ordem dos filtros |
| `auth/infrastructure/security/JwtUtil.java` | Geração e validação de tokens JWT |
| `auth/infrastructure/security/JwtAuthenticationFilter.java` | Filtro por requisição para autenticação via Bearer token |
| `auth/infrastructure/security/WebhookTokenFilter.java` | Filtro para o endpoint de webhook de orçamento |
| `auth/infrastructure/controller/AuthController.java` | Controller de login e registro |
| `auth/infrastructure/repository/UsuarioRepository.java` | Spring Data JPA para `Usuario` |
| `db/migration/V1__init_schema.sql` | Cria a tabela `usuarios` |

## Observações

- **Módulo simplificado por decisão explícita**: `auth` não possui camada `application`
  (sem use cases nem services) — acessa Spring Data e Spring Security diretamente. A
  constituição reconhece essa exceção como a única permitida para módulos de autenticação
  JWT stateless.
- **`WebhookTokenFilter` executado antes do `JwtAuthenticationFilter`**: a ordem no
  `SecurityConfig` é `webhookTokenFilter → jwtAuthFilter → UsernamePasswordAuthenticationFilter`.
  O webhook é autenticado apenas pelo token dedicado, sem necessitar de JWT.
- **Token JWT sem claims de autorização (roles)**: o token carrega somente o subject (email).
  Não há RBAC (Role-Based Access Control) — toda rota protegida é acessível a qualquer
  usuário autenticado.

---

## Pontos de Atenção

### Achados de Integridade Crítica

> **Curadoria (spec `010-auth-achados-criticos`)**: CRI-001 e CRI-002 foram **resolvidos**.
> O `ApiExceptionHandler` ganhou um handler genérico `Exception` → 500 com log SLF4J e mensagem
> segura; o `AuthController` passou a delegar o tratamento ao handler central e a validar a entrada
> na borda. Os achados de melhoria (MEL-001/002/003) permanecem como backlog.

#### [CRI-001] `POST /api/auth/register` retorna HTTP 500 para falhas genéricas ✅ RESOLVIDO

**Componente afetado**: `AuthController#register` · `ApiExceptionHandler`

**Descrição**: o bloco `catch (Exception e)` no método `register` retornava
`ResponseEntity.status(500).build()` sem log, sem mensagem e sem distinção de causa.
Qualquer exceção inesperada resultava em 500 silencioso — sem feedback ao cliente nem
rastreabilidade nos logs.

**Correção aplicada (spec `010`)**: removido o `catch (Exception e)` genérico do `register`; o
conflito de e-mail duplicado passou a lançar `ConflitoDeRecursoException` (**409**, via handler
central). Adicionado ao `ApiExceptionHandler` um `@ExceptionHandler(Exception.class)` que registra
log SLF4J com contexto e retorna **500** com mensagem genérica segura (sem stacktrace/segredos) —
rede de segurança central que beneficia todos os domínios. `login` e `register` passaram a emitir
logging estruturado de entrada/desfecho (sem registrar senha nem token).

**Estado**: resolvido.

---

#### [CRI-002] Ausência de validação de entrada no `AuthController` ✅ RESOLVIDO

**Componente afetado**: `AuthController#login`, `AuthController#register`, `LoginRequest`

**Descrição**: `LoginRequest` não possuía Bean Validation. Email `null` ou senha em branco
chegavam à camada de segurança, com risco de `NullPointerException` ou comportamento indefinido.

**Correção aplicada (spec `010`)**: `LoginRequest` recebeu `@NotBlank` + `@Email` no email e
`@NotBlank` na senha; os parâmetros de `login`/`register` foram anotados com `@Valid`. Entradas
inválidas são rejeitadas com **400** (via `MethodArgumentNotValidException` no `ApiExceptionHandler`)
antes de alcançar a autenticação. Credenciais bem-formadas porém incorretas seguem retornando **401**.
A política de força de senha (tamanho/complexidade) permanece backlog.

**Estado**: resolvido.

---

### Achados de Melhoria

#### [MEL-001] Ausência de controle de autorização (RBAC)

**Componente afetado**: `SecurityConfig`

**Descrição**: toda rota protegida é acessível a qualquer usuário autenticado — não há
roles, perfis ou permissões. Não é possível, por exemplo, restringir o cadastro de
funcionários a administradores.

**Sugestão de backlog**: definir roles (`ADMIN`, `FUNCIONARIO`, `CLIENTE`) no `Usuario`,
incluí-las como claims no JWT e configurar `@PreAuthorize` ou regras no `SecurityConfig`.

---

#### [MEL-002] TODO de refatoração documentado no código

**Componente afetado**: `AuthController.java`

**Descrição**: o arquivo contém `// TODO refatorar controller com boas práticas`. O módulo
não segue o padrão hexagonal completo (sem camada `application`), e o controller acessa
`UsuarioRepository` diretamente, violando a separação de responsabilidades.

**Sugestão de backlog**: extrair lógica de negócio do controller para um service
dedicado (`RegistrarUsuarioService`, `AutenticarUsuarioService`), seguindo o padrão dos
demais módulos — conforme alerta explícito da constituição (Débito — Refatoração pendente do `auth`).

---

#### [MEL-003] Sem mecanismo de revogação ou refresh de token

**Componente afetado**: `JwtUtil`, `JwtAuthenticationFilter`

**Descrição**: tokens JWT são stateless e não podem ser revogados antes da expiração. Não
há refresh token — ao expirar, o usuário reautentica. Um token comprometido permanece válido
até expirar (24h).

**Sugestão de backlog**: avaliar token blocklist (Redis) para revogação, ou refresh token
de curta duração com access token de longa duração, dependendo dos requisitos de segurança.
