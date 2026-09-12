# Diagramas de Sequência

Os dois fluxos que o enunciado pede: **autenticação por CPF** e **abertura de
ordem de serviço**. Ambos refletem o código como ele está, não como se pretendia
que fosse.

---

## 1 · Autenticação por CPF

Trocar um CPF por um token que as rotas protegidas aceitam. O cliente **não tem
senha** — a posse do CPF de um cliente ativo é a credencial.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant GW as API Gateway
    participant L as Lambda token
    participant SM as Secrets Manager
    participant DB as RDS PostgreSQL

    C->>GW: POST /auth<br/>{ "cpf": "529.982.247-25" }
    GW->>L: invoca (AWS_PROXY, payload 2.0)

    L->>L: normaliza e valida CPF<br/>por dígito verificador

    alt CPF estruturalmente inválido
        L-->>GW: 400 CPF_INVALIDO
        GW-->>C: 400
        Note over L,DB: não consulta o banco:<br/>rejeita antes de gastar conexão
    else CPF válido
        L->>SM: credenciais do RDS<br/>(cache no container)
        SM-->>L: usuário e senha
        L->>DB: SELECT id, nome, ativo FROM clientes<br/>WHERE documento normalizado = CPF
        DB-->>L: linha ou vazio
        Note over L,DB: a comparação usa regexp_replace sobre cpf_ou_cnpj,<br/>com índice funcional sobre a mesma expressão

        alt cliente não existe
            L-->>GW: 404 CLIENTE_NAO_ENCONTRADO
            GW-->>C: 404
        else cliente inativo
            L-->>GW: 403 CLIENTE_INATIVO
            GW-->>C: 403
        else cliente ativo
            L->>SM: segredo HMAC do JWT
            SM-->>L: segredo
            L->>L: assina HS256<br/>{ sub, tipo: CLIENTE, clienteId, nome, roles }
            L-->>GW: 200 { token, expiraEm, tipo, cliente }
            GW-->>C: 200 + token
        end
    end
```

**Por que a consulta normaliza na leitura.** A coluna `cpf_ou_cnpj` guarda o
documento **como foi digitado** — o domínio valida por dígito verificador mas
persiste a string original, então o mesmo CPF pode estar gravado como
`529.982.247-25` ou `52998224725`. Sem o `regexp_replace` a busca por dígitos não
encontraria o primeiro. Há um índice funcional sobre a mesma expressão, então
não cai em *sequential scan* — ver [`modelo-de-dados.md`](modelo-de-dados.md).

### Consumo de rota protegida

O token emitido acima atravessa duas validações independentes, com o **mesmo
segredo HMAC** e propósitos diferentes.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant GW as API Gateway
    participant AZ as Lambda authorizer
    participant SM as Secrets Manager
    participant NLB as NLB
    participant APP as API Spring Boot

    C->>GW: GET /clientes<br/>Authorization: Bearer <jwt>
    GW->>AZ: REQUEST authorizer (cache 300s)
    AZ->>SM: segredo HMAC
    SM-->>AZ: segredo
    AZ->>AZ: verifica assinatura e expiração<br/>HS256/384/512, comparação em tempo constante

    alt token ausente, expirado ou adulterado
        AZ-->>GW: { isAuthorized: false }
        GW-->>C: 403
        Note over GW,APP: a requisição nunca chega ao cluster —<br/>a proteção está na borda
    else token válido
        AZ-->>GW: { isAuthorized: true, context }
        GW->>NLB: encaminha + header x-request-id
        NLB->>APP: roteia para um pod
        APP->>APP: JwtAuthenticationFilter lê a claim "tipo"
        Note over APP: tipo = CLIENTE → autentica pelas claims,<br/>sem consultar a tabela usuarios
        APP-->>C: 200
    end
```

**Duas validações não é redundância.** O authorizer protege a **borda** — sem
ele a requisição chegaria ao cluster antes de ser recusada. O filtro da
aplicação estabelece a **identidade** no contexto de segurança do Spring, que é
o que permite a autorização por papel dentro do domínio. Um cobre o que o outro
não faz.

**Por que a claim `tipo`.** A aplicação tem um segundo fluxo de autenticação, por
e-mail e senha, cujo token traz o e-mail no `sub` e resolve o usuário na tabela
`usuarios`. O token de CPF traz `tipo: "CLIENTE"`, e por essa claim o filtro
monta a autenticação a partir das próprias claims — um cliente não é usuário do
sistema e exigir que fosse seria acoplamento desnecessário.

---

## 2 · Abertura de ordem de serviço

Da requisição autenticada até a notificação no e-mail do cliente. O trecho de
notificação é **assíncrono**: segue o padrão *outbox*, com persistência antes do
envio e reprocessamento agendado.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant GW as API Gateway
    participant APP as OrdemDeServicoController
    participant SVC as CriarNovaOrdemDeServicoService
    participant DB as RDS PostgreSQL
    participant EV as ApplicationEventPublisher
    participant NOT as EnviarNotificacaoStatusOSService
    participant ENT as EntregarNotificacaoService
    participant SES as Amazon SES

    C->>GW: POST /ordens-servico + Bearer<br/>{ clienteId, funcionarioId, placaVeiculo }
    GW->>APP: autorizado, encaminhado ao NLB

    APP->>SVC: criarNovaOrdemDeServico(command)

    rect rgb(235, 241, 236)
        Note over SVC,DB: validações — qualquer falha aborta antes de persistir
        SVC->>DB: buscar cliente por id
        DB-->>SVC: Cliente
        SVC->>DB: buscar veículo por placa
        DB-->>SVC: Veiculo
        SVC->>DB: buscar funcionário por id
        DB-->>SVC: Funcionario
        SVC->>DB: reservar peças (estoque)
        DB-->>SVC: ok
    end

    SVC->>DB: INSERT ordens_de_servico<br/>+ snapshot de cliente, veículo e funcionário
    DB-->>SVC: número da OS

    SVC->>EV: publica evento de situação alterada
    SVC-->>APP: número da OS
    APP-->>C: 201 Created

    Note over EV,SES: daqui em diante é assíncrono —<br/>o cliente já recebeu a resposta

    EV->>NOT: OrdemDeServicoSituacaoAlteradaEm
    NOT->>DB: INSERT notificacoes (status PENDENTE)
    NOT->>ENT: entregar
    ENT->>SES: SendEmail (credencial via IRSA)

    alt envio bem-sucedido
        SES-->>ENT: ok
        ENT->>DB: UPDATE notificacoes SET status = ENVIADA
    else falha no envio
        SES-->>ENT: erro
        ENT->>DB: UPDATE status = FALHA, tentativas += 1
        Note over ENT,DB: ReprocessamentoNotificacoesScheduler<br/>reprocessa periodicamente até o limite
    end
```

### Decisões visíveis neste fluxo

**A OS grava um *snapshot* do cliente, do veículo e do funcionário**, além das
chaves estrangeiras. Uma ordem de serviço é documento histórico: renomear um
cliente hoje não pode reescrever o que foi emitido no ano passado. É
denormalização deliberada — registrada em
[`../adr/0002-snapshot-em-ordens-e-orcamentos.md`](../adr/0002-snapshot-em-ordens-e-orcamentos.md).

**A notificação é persistida antes de ser enviada.** Se o SES estiver fora do ar,
a linha fica em `PENDENTE`/`FALHA` com contador de tentativas, e o scheduler
reprocessa. A abertura da OS **não falha** porque o e-mail falhou — são
transações separadas, e é isso que o padrão outbox compra.

**O evento só é publicado quando a situação muda de fato.** Duas situações
internas podem mapear para o mesmo status externo (`DIAGNOSTICO_EM_ANDAMENTO` e
`DIAGNOSTICO_CONCLUIDO` viram "Diagnóstico"); publicar nos dois casos geraria
e-mail duplicado. A comparação `situacaoAnterior != situacaoAtual` em
`ConcluirDiagnosticoService` é o que evita isso.

**As peças são reservadas antes da OS existir.** Se a reserva falhar por estoque
insuficiente, nada é persistido. O inverso — criar a OS e depois descobrir que
não há peça — deixaria ordem órfã.
