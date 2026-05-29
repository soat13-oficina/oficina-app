# Fluxograma do Projeto — Oficina API

## 1. Visão Geral da Arquitetura

```mermaid
flowchart TD
    CLIENT([Cliente HTTP]) -->|JWT Bearer Token| SEC[JwtAuthenticationFilter]
    SEC --> ROUTER{Router}

    ROUTER --> AUTH[AuthController]
    ROUTER --> CLI[ClienteController]
    ROUTER --> VEI[VeiculoController]
    ROUTER --> OS[OrdemDeServicoController]
    ROUTER --> ORC[OrcamentoController]
    ROUTER --> PEC[PecaInsumoController]

    AUTH -->|sem JWT| UsuarioRepo[(usuarios)]
    CLI --> CLI_SVC[ServicesCliente]
    VEI --> VEI_SVC[ServicesVeiculo]
    OS --> OS_SVC[ServicesOrdemDeServico]
    ORC --> ORC_SVC[ServicesOrcamento]
    PEC --> PEC_SVC[ServicesPecaInsumo]

    CLI_SVC --> CLI_REPO[(clientes)]
    VEI_SVC --> VEI_REPO[(veiculos)]
    OS_SVC --> OS_REPO[(ordens de servico)]
    OS_SVC --> FUNC_REPO[(funcionarios)]
    ORC_SVC --> ORC_REPO[(orcamentos)]
    PEC_SVC --> PEC_REPO[(pecas insumos)]
```

---

## 2. Arquitetura Hexagonal por Módulo

```mermaid
flowchart LR
    subgraph WEB["Infrastructure - Web"]
        CTRL["Controller REST"]
        REQ["Request DTO record"]
        RESP["Response DTO record+from"]
    end

    subgraph APP["Application"]
        UC["UseCase interface"]
        SVC["Service implementacao"]
        CMD[Command]
        QRY[Query]
    end

    subgraph DOM["Domain"]
        ENT["Entidade factory methods"]
        REPO_I["Repository interface port"]
    end

    subgraph INFRA["Infrastructure - Persistence"]
        JPA_REPO["JpaRepository adapter"]
        SD_REPO["SpringDataRepository"]
        DB[(PostgreSQL)]
    end

    CTRL --> REQ
    CTRL --> UC
    SVC --> CMD
    SVC --> QRY
    SVC --> ENT
    SVC --> REPO_I
    UC --> SVC
    JPA_REPO --> REPO_I
    JPA_REPO --> SD_REPO
    SD_REPO --> DB
    CTRL --> RESP
```

---

## 3. Ciclo de Vida da Ordem de Serviço

```mermaid
flowchart TD
    A([Inicio]) --> B["POST /ordens-servico"]
    B --> OS_ABERTA([OS_ABERTA])

    OS_ABERTA -->|"PUT /{numero}"| OS_ABERTA
    OS_ABERTA -->|"DELETE /{numero}"| EXCLUIDA([Excluida])
    OS_ABERTA -->|"POST .../diagnostico/iniciar"| DIAG_AND([DIAGNOSTICO_EM_ANDAMENTO])

    DIAG_AND -->|"POST .../diagnostico/concluir"| DIAG_CONC([DIAGNOSTICO_CONCLUIDO])
    DIAG_CONC -->|"POST .../diagnostico/enviar-para-orcamento"| ORC_GER([ORCAMENTO_GERADO])
    ORC_GER -->|"POST .../aguardar-aprovacao"| AGU_APR([AGUARDANDO_APROVACAO])
    AGU_APR -->|"POST .../execucao/iniciar"| SERV_AND([SERVICO_EM_ANDAMENTO])
    SERV_AND -->|"POST .../finalizacao"| OS_FIN([OS_FINALIZADA])
    OS_FIN -->|"POST .../entrega"| ENTREGUE([ENTREGUE])

    style OS_ABERTA fill:#4CAF50,color:#fff
    style OS_FIN fill:#2196F3,color:#fff
    style ENTREGUE fill:#1565C0,color:#fff
    style EXCLUIDA fill:#f44336,color:#fff
    style DIAG_AND fill:#FF9800,color:#fff
    style DIAG_CONC fill:#FF9800,color:#fff
    style ORC_GER fill:#9C27B0,color:#fff
    style AGU_APR fill:#FF9800,color:#fff
    style SERV_AND fill:#FF9800,color:#fff
```

---

## 3.1 Status previstos vs fluxo implementado hoje

O enum `StatusOrdemDeServico` contém os estados abaixo:

- `OS_ABERTA`
- `DIAGNOSTICO_EM_ANDAMENTO`
- `DIAGNOSTICO_CONCLUIDO`
- `AGUARDANDO_ORCAMENTO`
- `ORCAMENTO_GERADO`
- `AGUARDANDO_APROVACAO`
- `ORCAMENTO_APROVADO`
- `SERVICO_EM_ANDAMENTO`
- `AGUARDANDO_PECA`
- `SERVICO_CONCLUIDO`
- `OS_FINALIZADA`
- `ENTREGUE`

As transições efetivamente implementadas no agregado `OrdemDeServico` nesta versão do projeto são:

- `OS_ABERTA -> DIAGNOSTICO_EM_ANDAMENTO`
- `DIAGNOSTICO_EM_ANDAMENTO -> DIAGNOSTICO_CONCLUIDO`
- `DIAGNOSTICO_CONCLUIDO -> ORCAMENTO_GERADO`
- `ORCAMENTO_GERADO -> AGUARDANDO_APROVACAO` (novo passo: cliente precisa aprovar o orçamento)
- `AGUARDANDO_APROVACAO -> SERVICO_EM_ANDAMENTO` (novo passo: orçamento aprovado, execução iniciada)
- `SERVICO_EM_ANDAMENTO -> OS_FINALIZADA`
- `OS_FINALIZADA -> ENTREGUE`

Com isso o fluxo principal passa explicitamente por `AGUARDANDO_APROVACAO` e `SERVICO_EM_ANDAMENTO`, que antes existiam apenas no enum. A finalização agora exige que a ordem esteja em `SERVICO_EM_ANDAMENTO` (não mais diretamente a partir de `ORCAMENTO_GERADO`).

Os estados auxiliares `AGUARDANDO_ORCAMENTO`, `ORCAMENTO_APROVADO`, `AGUARDANDO_PECA` e `SERVICO_CONCLUIDO` continuam previstos no enum para evoluções futuras, mas ainda não possuem transições próprias implementadas no domínio de `OrdemDeServico` nesta revisão.

---

## 3.2 Timestamps e Métrica de Execução

```mermaid
flowchart LR
    INI["iniciadaEm"] --> DUR["Duration.between(iniciadaEm, finalizadaEm)"]
    FIM["finalizadaEm"] --> DUR
    DUR --> MEDIA["GET /ordens-servico/metricas/tempo-medio"]
    MEDIA --> RESP["tempoMedioExecucaoEmSegundos\n tempoMedioExecucaoFormatado\n quantidadeOrdensConsideradas"]
    ENTREGA["entregueEm"] -.->|"nao entra no calculo"| MEDIA
```

Regras atuais da métrica:

- Considera ordens com `iniciadaEm` e `finalizadaEm` preenchidos.
- Ordens em `OS_FINALIZADA` e `ENTREGUE` entram no cálculo.
- `entregueEm` é apenas informativo para rastrear a entrega ao cliente e não altera a duração de execução.

---

## 4. Ciclo de Vida do Orçamento

```mermaid
flowchart LR
    A(["Criacao via EnviarDiagnostico\nou POST /orcamentos"]) --> AGUA([AGUARDANDO_APROVACAO])
    AGUA -->|"POST /orcamentos/{id}/aprovacao"| APR([APROVADO])
    AGUA -->|"POST /orcamentos/{id}/rejeicao"| REJ([REJEITADO])

    style AGUA fill:#FF9800,color:#fff
    style APR fill:#4CAF50,color:#fff
    style REJ fill:#f44336,color:#fff
```

---

## 5. Estoque de Peças e Insumos

```mermaid
flowchart TD
    PECA["PecaInsumo\nid: String\ndescricao\nquantidadeEstoque\nquantidadeReservada"]

    PECA -->|getQuantidadeDisponivel| CALC["disponivel = estoque - reservada"]

    subgraph OPERACOES["Operacoes de Estoque"]
        ADD["POST /adicionar-estoque\nAdicionarEstoquePecaService"]
        REM["POST /remover-estoque\nRemoverEstoquePecaService"]
        RES["POST /reservar\nReservarPecaService"]
        LIB["POST /liberar-reserva\nLiberarReservaPecaService"]
        CON["POST /consumir\nConsumirPecaService"]
    end

    ADD -->|incrementa estoque| PECA
    REM -->|decrementa estoque| PECA
    RES -->|incrementa reservada| PECA
    LIB -->|decrementa reservada| PECA
    CON -->|decrementa estoque e reservada| PECA
```

---

## 6. Módulo de Autenticação

```mermaid
flowchart LR
    A([Requisicao]) --> B{Rota publica?}
    B -->|"Sim: /api/auth/register ou /login"| C[AuthController]
    B -->|Nao| D[JwtAuthenticationFilter]

    C -->|register| E["BCrypt senha"]
    E --> F[(usuarios)]
    C -->|login| G[Valida credenciais]
    G --> H["JwtUtil generateToken"]
    H --> I(["JWT Token expira em 24h"])

    D --> J["JwtUtil validateToken"]
    J -->|valido| K[SecurityContext]
    K --> L([Controller protegido])
    J -->|invalido| M([401 Unauthorized])
```

---

## 7. Relacionamento entre Módulos

```mermaid
flowchart TD
    AUTH[auth] -.->|protege| CLI
    AUTH -.->|protege| VEI
    AUTH -.->|protege| OS
    AUTH -.->|protege| ORC
    AUTH -.->|protege| PEC

    CLI["cliente\nCRUD CPF/CNPJ"] -->|clienteId| OS
    CLI -->|clienteId| VEI
    VEI["veiculo\nCRUD placa"] -->|veiculoId| OS
    OS["ordemservico\nfluxo principal"] -->|gera orçamento| ORC
    ORC -->|aprovação reserva peças| PEC
    OS -->|finalização consome peças reservadas| PEC
    OS -->|expõe métrica| METRIC["GET /ordens-servico/metricas/tempo-medio"]

    COMMON["common\nExceptions + Handler"] -.->|trata erros| CLI
    COMMON -.->|trata erros| VEI
    COMMON -.->|trata erros| OS
    COMMON -.->|trata erros| ORC
    COMMON -.->|trata erros| PEC
```

---

## 8. Mudanças Recentes Registradas no Fluxo

Últimas mudanças incorporadas ao projeto e refletidas neste documento:

- Fechamento do fluxo principal da ordem de serviço com os passos `AGUARDANDO_APROVACAO` e `SERVICO_EM_ANDAMENTO`, expostos pelos endpoints `POST /ordens-servico/{numeroOrdemServico}/aguardar-aprovacao` e `POST /ordens-servico/{numeroOrdemServico}/execucao/iniciar`. A finalização passou a exigir o status `SERVICO_EM_ANDAMENTO`.
- Correção do rollback de reservas na aprovação de orçamento: quando falta estoque para alguma peça, as reservas já feitas são efetivamente liberadas via `LiberarReservaPecaUseCase`, e não mais deixadas presas.
- Validação de CPF/CNPJ do cliente passou a conferir o dígito verificador (biblioteca `caelum-stella`), em vez de apenas contar dígitos.
- Testes de integração de persistência migrados para Testcontainers (PostgreSQL), removendo a dependência de um banco em `localhost`.
- Inclusão da transição `OS_FINALIZADA -> ENTREGUE` com endpoint `POST /ordens-servico/{numeroOrdemServico}/entrega`.
- Persistência do timestamp `entregueEm` na ordem de serviço.
- Inclusão do endpoint `GET /ordens-servico/metricas/tempo-medio`.
- Cálculo da média geral de execução com base em `iniciadaEm` e `finalizadaEm`.
- Atualização do Swagger para documentar entrega ao cliente, métrica de tempo médio e exemplos compatíveis com o fluxo atual.
