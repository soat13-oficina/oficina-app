# Fluxograma do Projeto — Oficina API

## 1. Visão Geral da Arquitetura

```mermaid
flowchart TD
    CLIENT([Cliente HTTP]) -->|JWT Bearer Token| SEC[JwtAuthenticationFilter]
    SEC --> ROUTER{Router}

    ROUTER --> AUTH[AuthController\n/api/auth]
    ROUTER --> CLI[ClienteController\n/clientes]
    ROUTER --> VEI[VeiculoController\n/veiculos]
    ROUTER --> OS[OrdemDeServicoController\n/ordens-servico]
    ROUTER --> ORC[OrcamentoController\n/orcamentos]
    ROUTER --> PEC[PecaInsumoController\n/pecas-insumos]

    AUTH -->|sem JWT| UsuarioRepo[(usuarios)]
    CLI --> CLI_SVC[Services\nCliente]
    VEI --> VEI_SVC[Services\nVeiculo]
    OS --> OS_SVC[Services\nOrdemDeServico]
    ORC --> ORC_SVC[Services\nOrcamento]
    PEC --> PEC_SVC[Services\nPecaInsumo]

    CLI_SVC --> CLI_REPO[(clientes)]
    VEI_SVC --> VEI_REPO[(veiculos)]
    OS_SVC --> OS_REPO[(ordens_de_servico)]
    OS_SVC --> FUNC_REPO[(funcionarios)]
    ORC_SVC --> ORC_REPO[(orcamentos)]
    PEC_SVC --> PEC_REPO[(pecas_insumos)]
```

---

## 2. Arquitetura Hexagonal por Módulo

```mermaid
flowchart LR
    subgraph WEB["Infrastructure — Web"]
        CTRL[Controller\nREST]
        REQ[Request DTO\nrecord]
        RESP[Response DTO\nrecord + from]
    end

    subgraph APP["Application"]
        UC[UseCase\ninterface]
        SVC[Service\nimplementação]
        CMD[Command]
        QRY[Query]
    end

    subgraph DOM["Domain"]
        ENT[Entidade\nfactory methods]
        REPO_I[Repository\ninterface port]
    end

    subgraph INFRA["Infrastructure — Persistence"]
        JPA_REPO[JpaRepository\nadapter]
        SD_REPO[SpringDataRepository\nextends JpaRepository]
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
    A([Início]) --> B[POST /ordens-servico]
    B --> OS_ABERTA([OS_ABERTA])

    OS_ABERTA -->|PUT /ordens-servico/num| OS_ABERTA
    OS_ABERTA -->|DELETE /ordens-servico/num| EXCLUIDA([Excluída])
    OS_ABERTA -->|POST .../diagnostico/iniciar| DIAG_AND([DIAGNOSTICO_EM_ANDAMENTO])

    DIAG_AND -->|POST .../diagnostico/concluir| DIAG_CONC([DIAGNOSTICO_CONCLUIDO])

    DIAG_CONC -->|POST .../enviar-para-orcamento| AGUA_ORC([AGUARDANDO_ORCAMENTO])

    AGUA_ORC --> ORC_GER([ORCAMENTO_GERADO])
    ORC_GER --> AGUA_APR([AGUARDANDO_APROVACAO])
    AGUA_APR -->|Aprovado| ORC_APR([ORCAMENTO_APROVADO])
    AGUA_APR -->|Rejeitado| REJEITADO([Rejeitado])
    ORC_APR --> SERV_AND([SERVICO_EM_ANDAMENTO])
    SERV_AND --> AGUA_PECA([AGUARDANDO_PECA])
    AGUA_PECA --> SERV_AND
    SERV_AND --> SERV_CONC([SERVICO_CONCLUIDO])
    SERV_CONC -->|POST .../finalizacao| OS_FIN([OS_FINALIZADA])

    style OS_ABERTA fill:#4CAF50,color:#fff
    style OS_FIN fill:#2196F3,color:#fff
    style EXCLUIDA fill:#f44336,color:#fff
    style REJEITADO fill:#f44336,color:#fff
    style DIAG_AND fill:#FF9800,color:#fff
    style DIAG_CONC fill:#FF9800,color:#fff
    style AGUA_ORC fill:#9C27B0,color:#fff
    style ORC_GER fill:#9C27B0,color:#fff
    style AGUA_APR fill:#9C27B0,color:#fff
    style ORC_APR fill:#009688,color:#fff
    style SERV_AND fill:#009688,color:#fff
    style AGUA_PECA fill:#FF5722,color:#fff
    style SERV_CONC fill:#009688,color:#fff
```

---

## 4. Ciclo de Vida do Orçamento

```mermaid
flowchart LR
    A([Criação via\nEnviarDiagnostico\nouPOST /orcamentos]) --> AGUA([AGUARDANDO_APROVACAO])
    AGUA -->|aprovar| APR([APROVADO])
    AGUA -->|rejeitar| REJ([REJEITADO])

    style AGUA fill:#FF9800,color:#fff
    style APR fill:#4CAF50,color:#fff
    style REJ fill:#f44336,color:#fff
```

---

## 5. Estoque de Peças e Insumos

```mermaid
flowchart TD
    PECA[PecaInsumo\nid: String\ndescricao\nquantidadeEstoque\nquantidadeReservada]

    PECA -->|getQuantidadeDisponivel| CALC["disponivel =\nestoque - reservada"]

    subgraph OPERACOES["Operações de Estoque"]
        ADD[POST /adicionar-estoque\nAdicionarEstoquePecaService]
        REM[POST /remover-estoque\nRemoverEstoquePecaService]
        RES[POST /reservar\nReservarPecaService]
        LIB[POST /liberar-reserva\nLiberarReservaPecaService]
    end

    ADD -->|incrementa estoque| PECA
    REM -->|decrementa estoque| PECA
    RES -->|incrementa reservada| PECA
    LIB -->|decrementa reservada| PECA
```

---

## 6. Módulo de Autenticação

```mermaid
flowchart LR
    A([Requisição]) --> B{Rota pública?}
    B -->|Sim\n/api/auth/register\n/api/auth/login| C[AuthController]
    B -->|Não| D[JwtAuthenticationFilter]

    C -->|register| E[BCrypt senha]
    E --> F[(usuarios)]
    C -->|login| G[Valida credenciais]
    G --> H[JwtUtil.generateToken]
    H --> I([JWT Token\nexpira em 24h])

    D --> J[JwtUtil.validateToken]
    J -->|válido| K[SecurityContext]
    K --> L([Controller protegido])
    J -->|inválido| M([401 Unauthorized])
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

    CLI[cliente\nCRUD + CPF/CNPJ] -->|clienteId| OS
    CLI -->|clienteId| VEI
    VEI[veiculo\nCRUD + placa] -->|veiculoId| OS
    OS[ordemservico\nfluxo principal] -->|cria| ORC
    PEC[pecainsumo\nestoque] -.->|reserva para| OS

    COMMON[common\nExceptions + Handler] -.->|trata erros| CLI
    COMMON -.->|trata erros| VEI
    COMMON -.->|trata erros| OS
    COMMON -.->|trata erros| ORC
    COMMON -.->|trata erros| PEC
```
