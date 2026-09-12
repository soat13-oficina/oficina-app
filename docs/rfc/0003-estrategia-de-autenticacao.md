# RFC 0003 — Estratégia de autenticação

- **Status:** Aceito
- **Data:** 2026-09-12
- **Escopo:** `oficina-lambda-auth`, `oficina-app`

## Contexto

O enunciado exige três coisas encadeadas:

1. Um **API Gateway** protegendo as rotas sensíveis da aplicação
2. Autenticação **via CPF**
3. Uma **Function Serverless** que valide o CPF, consulte existência e status do
   cliente na base e devolva um **JWT** válido para consumo das APIs protegidas

A restrição que molda tudo: a aplicação **já tem** um mecanismo de autenticação
próprio, herdado da Fase 2 — login por e-mail e senha, JWT assinado com `jjwt`,
usuários na tabela `usuarios`, e um `JwtAuthenticationFilter` que resolve a
identidade por e-mail. Não se trata de criar autenticação, e sim de **acrescentar
um segundo fluxo sem quebrar o primeiro**.

## Proposta

**JWT HMAC com segredo compartilhado, validado por um Lambda Authorizer no
gateway e novamente pelo filtro da aplicação.**

```
CPF → Lambda token → valida dígito verificador
                   → consulta cliente e status no RDS
                   → assina HS256 com segredo do Secrets Manager
                   → JWT { sub: cpf, tipo: CLIENTE, clienteId, nome, roles }

JWT → API Gateway → Lambda authorizer valida (borda)
                  → NLB → aplicação valida de novo (identidade)
```

O discriminador é a claim **`tipo`**. Quando vale `CLIENTE`, o filtro da
aplicação monta a autenticação a partir das próprias claims, sem procurar o CPF
na tabela `usuarios` — um cliente não é usuário do sistema. Quando ausente, o
fluxo original de e-mail e senha segue intacto.

## Alternativas consideradas

| Alternativa | Avaliação |
|---|---|
| **Amazon Cognito** | Resolveria o JWKS de graça e traria o autorizador nativo junto. Mas o enunciado pede explicitamente uma *Function Serverless* que valide o CPF e consulte o status do cliente na base — regra que não cabe num user pool sem, de novo, uma Lambda de trigger. Acrescentaria um serviço sem remover a função. |
| **JWT Authorizer nativo do API Gateway** (RS256 + JWKS) | Seria a opção óbvia e é **inaplicável**: o autorizador nativo exige um emissor OIDC com JWKS publicado, ou seja, chave assimétrica. Um segredo HMAC não tem JWKS. |
| **Migrar tudo para RS256 + JWKS próprio** | Correto do ponto de vista de arquitetura — a aplicação validaria com a chave pública, sem compartilhar segredo. Exigiria hospedar o endpoint JWKS, gerenciar rotação de chave e alterar a validação da aplicação. Três frentes a mais numa fase que já mexe em quatro repositórios. Registrado como evolução natural. |
| **Validar o JWT apenas na aplicação** | Mais simples e descumpre o requisito: as rotas sensíveis ficariam expostas no gateway, que passaria a ser proxy burro. |
| **Validar apenas no authorizer** | Deixaria o `SecurityContext` do Spring vazio, inviabilizando autorização por papel dentro do domínio. |

## Detalhes que a implementação obrigou a decidir

**Assinar em HS256, verificar HS256, HS384 e HS512.** A `jjwt`, usada pela
aplicação, escolhe o algoritmo pelo **tamanho da chave**: com o segredo de 64
caracteres gerado pelo Terraform (512 bits), ela assina em HS512. Um authorizer
restrito a HS256 recusaria no gateway todo token emitido pelo login de e-mail e
senha. A assimetria é proposital.

**O segredo é criado pelo Terraform da Lambda**, em
`oficina/<ambiente>/jwt-secret` no Secrets Manager, e lido pela pipeline da
aplicação para popular o `JWT_SECRET` do cluster. A camada de autenticação é a
fonte da verdade.

**A rota `POST /auth` é pública por definição** — é onde o token é obtido. Todo o
resto passa pelo authorizer, com cache de 300 s por token para não invocar a
função a cada requisição.

**A função de token roda dentro da VPC** (precisa alcançar o RDS); o authorizer
roda **fora** dela (só precisa do Secrets Manager). Anexar o authorizer à VPC
criaria uma ENI por container e somaria cold start no caminho crítico de toda
requisição protegida.

## Impacto

**Positivo**

- A proteção fica na **borda**: requisição sem token válido recebe 403 do gateway e nunca alcança o cluster.
- O mesmo segredo serve aos dois lados sem chamada de rede entre eles — a aplicação valida o token localmente.
- O fluxo de e-mail e senha continua funcionando sem alteração de comportamento.
- Biblioteca de JWT escrita com `node:crypto`, sem dependência externa: pacote menor, cold start menor e nenhuma CVE de terceiro para acompanhar. Cobre comparação em tempo constante e recusa explícita de `alg` inesperado, com 8 testes só para isso.

**Negativo e mitigações**

- **Segredo compartilhado entre dois repositórios.** Mitigado por nome de contrato documentado e leitura em tempo de deploy — o valor nunca aparece em código.
- **Rotação invalida tudo.** Um `destroy` + `apply` da camada de autenticação gera segredo novo, e a aplicação precisa de novo deploy para ressincronizar. Na prática, o sintoma é opaco: assinatura válida no gateway, recusada pela aplicação. Está documentado no roteiro de operação.
- **Ausência de senha é o modelo de ameaça.** Conhecer o CPF de um cliente ativo basta para obter token. É o que o enunciado pede, e é aceitável no escopo — mas em um sistema real exigiria segundo fator ou vínculo com um canal já autenticado.
- **O gateway bloqueia `/api/auth/*`.** Com o authorizer ligado, a rota de login da própria aplicação cai sob a regra do proxy e fica inacessível por ele. Contornável pelo NLB direto; a correção adequada é uma rota pública explícita para esse prefixo.

## Questões em aberto

- **Migrar para RS256 com JWKS**, eliminando o segredo compartilhado. É a evolução natural e já tem caminho desenhado.
- **Autorização por papel.** O token traz `roles: ["CLIENTE"]`, mas o `SecurityConfig` usa `anyRequest().authenticated()` — qualquer identidade autenticada alcança qualquer rota. O dado para restringir já existe; falta a regra.
- **Expor `/api/auth/*` como rota pública no gateway**, resolvendo a assimetria acima.
