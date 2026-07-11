# Oficina API - Tech Challenge

Backend monolítico para gestão de Oficina Mecânica, desenvolvido com **Java 21 + Spring Boot 4 + PostgreSQL**.

## Fase 2 — objetivo desta fase

Na Fase 1 o foco foi o domínio (OS, clientes, veículos, peças). Nesta Fase 2 o
objetivo é evoluir a base para suportar **maior demanda e múltiplas unidades**
com resiliência e escalabilidade, incorporando:

- Containerização revisada (Docker/Docker Compose).
- Orquestração via Kubernetes na AWS — EKS (Deployments, Services, ConfigMaps/Secrets, HPA).
- Infraestrutura como código via Terraform (VPC, cluster EKS, ECR e banco RDS).
- Pipeline de CI/CD (build, testes, imagem Docker no ECR, deploy no EKS).

A seção [Infraestrutura e Deploy](#infraestrutura-e-deploy) detalha os
componentes provisionados e como executar cada etapa.

---

## Pré-requisitos

- Java 21+
- Docker e Docker Compose
- Maven (ou use o wrapper `./mvnw` incluso no projeto)

---

## Subindo o ambiente

### 0. (Opcional) Customizar credenciais locais

```bash
cp .env.example .env
```

O `docker-compose.yml` lê as variáveis do `.env` (se não existir, usa os
defaults abaixo). O `.env` é ignorado pelo git.

### 1. Iniciar o banco de dados

```bash
docker-compose up -d db
```

Isso sobe um container PostgreSQL com as credenciais padrão:
- **Host:** `localhost:5432`
- **Banco:** `oficina_db`
- **Usuário:** `oficina_user`
- **Senha:** `oficina_123`

### 2. Iniciar a aplicação

```bash
./mvnw spring-boot:run
```

Alternativamente, `docker-compose up -d` sobe banco **e** aplicação
containerizados (usa o `Dockerfile` multi-stage do projeto).

O Flyway executa as migrations automaticamente na inicialização. A aplicação fica disponível em `http://localhost:8080`.

### 3. (Opcional) Visualizar e-mails com o Mailhog

O `docker-compose.yml` também sobe um serviço `mailhog` — um servidor SMTP
falso para desenvolvimento local. Ele **não envia e-mails de verdade**: toda
notificação disparada pela aplicação (via outbox, a cada mudança de status da
OS) é capturada por ele e exibida em uma UI web, em vez de ir para uma caixa
de entrada real.

```bash
docker-compose up -d mailhog
```

- **SMTP (usado pela aplicação):** `localhost:1025` — já é o default de
  `SMTP_HOST`/`SMTP_PORT` no `application.yml`, então nenhuma configuração
  extra é necessária.
- **UI web (para inspecionar os e-mails capturados):** http://localhost:8025

Para testar: dispare qualquer ação que gere notificação (ex.: uma mudança de
status de OS) e abra a UI do Mailhog em `http://localhost:8025` — o e-mail
capturado aparece na lista, com assunto, corpo e destinatário, sem precisar de
nenhuma conta de e-mail real.

---

## Usando o Swagger

Com a aplicação rodando, acesse:

**http://localhost:8080/swagger-ui/index.html**

Todos os endpoints (exceto `/api/auth/**`) exigem autenticação JWT. Para usar o Swagger:

**Passo 1 — Criar um usuário**

Localize o endpoint `POST /api/auth/register` e execute com:
```json
{
  "email": "admin@oficina.com",
  "senha": "123456"
}
```

**Passo 2 — Fazer login**

Execute `POST /api/auth/login` com as mesmas credenciais. Copie o valor do campo `token` da resposta.

**Passo 3 — Autorizar no Swagger**

1. Clique no botão **Authorize** (cadeado no topo da página)
2. No campo **bearerAuth**, cole o token copiado (apenas o valor, sem o prefixo `Bearer`)
3. Clique em **Authorize** e depois **Close**

A partir deste passo todos os endpoints enviam o JWT automaticamente.

---

## Testando via Insomnia

Importe o arquivo `tests/oficina-api.insomnia.json` no Insomnia.

Configure as variáveis de ambiente da collection:
| Variável | Valor padrão | Descrição |
|---|---|---|
| `base_url` | `http://localhost:8080` | URL base da API |
| `token` | _(vazio)_ | Token JWT — preencher após login |
| `cliente_id` | _(vazio)_ | UUID do cliente cadastrado |
| `veiculo_placa` | `ABC1D23` | Placa para testes |
| `numero_os` | _(vazio)_ | Número da OS criada |
| `numero_orcamento` | _(vazio)_ | Número do orçamento criado |

**Fluxo sugerido:**
1. `POST /api/auth/register` — criar usuário
2. `POST /api/auth/login` — copiar o token para a variável `token`
3. `POST /clientes` — criar cliente, copiar o `id` para `cliente_id`
4. `POST /veiculos` — criar veículo vinculado ao cliente
5. `POST /ordens-servico` — abrir OS, copiar o número para `numero_os`
6. Avançar o fluxo da OS (iniciar diagnóstico → concluir → enviar para orçamento)

---

## Executando os testes

```bash
./mvnw test
```

Esse comando roda toda a suíte. Cobertura mínima de 80% (linhas e branches) é verificada pelo JaCoCo. O build falha se a cobertura não for atingida.

### Testes unitários e de controller

Utilizam **H2 in-memory** — não exigem banco externo nem Docker.

### Testes de integração (PostgreSQL real via Testcontainers)

Os testes `*IntegrationTest` validam os repositórios JPA contra um **PostgreSQL real provisionado automaticamente pelo [Testcontainers](https://testcontainers.com/)**. Basta ter o **Docker em execução** — não é necessário subir o banco manualmente (`docker-compose`) nem apontar para um banco externo em `localhost`. O container é efêmero e descartado ao fim da execução, garantindo que os testes rodem em qualquer máquina/CI com Docker.

Detalhes:
- A classe base `PostgresIntegrationTest` (em `src/test/.../support`) sobe o container `postgres:16-alpine` e injeta o datasource via `@ServiceConnection`.
- Os testes usam `@Transactional` com rollback automático — nenhum dado persiste entre testes.
- Sem Docker disponível, apenas os testes de integração falham ao iniciar o container; os demais continuam rodando normalmente.

### Build completo com relatório de cobertura

```bash
./mvnw verify
```

O relatório HTML é gerado em `target/site/jacoco/index.html`.

---

## Scan de Vulnerabilidades e Qualidade (SonarQube)

O projeto está configurado para consolidar a cobertura de testes do JaCoCo e inspecionar a qualidade do código (code smells, bugs e vulnerabilidades) via **SonarQube**.

**Como executar a varredura:**

1. Certifique-se de que possui um servidor SonarQube rodando. Caso não possua, você pode subir um localmente usando Docker:
   ```bash
   docker run -d --name sonarqube -p 9000:9000 sonarqube:latest
   ```

2. Execute o build junto com os testes para que os relatórios do XML de cobertura do JaCoCo sejam criados (localizados na pasta do `target`):
   ```bash
   ./mvnw clean verify
   ```

3. Dispare a execução do *Sonar Scanner* passando a URL e as credenciais do seu servidor. **Atenção:** Se estiver usando o **PowerShell** no Windows, coloque os parâmetros com `-D` entre aspas para evitar erros de leitura, como no exemplo abaixo:
   ```bash
   ./mvnw sonar:sonar "-Dsonar.host.url=http://localhost:9000" "-Dsonar.token=SEU_TOKEN" "-Dsonar.projectKey=oficina"
   ```

Acesse o painel do seu SonarQube na URL do host para visualizar o nível de cobertura, a aba de "Security" e demais métricas de vulnerabilidade do projeto.

---

## Autenticação JWT

Todas as rotas exceto `/api/auth/register` e `/api/auth/login` exigem o header:

```
Authorization: Bearer <token>
```

O token expira em **24 horas**. Para renovar, basta fazer login novamente.

---

## Endpoints REST

| Domínio | Endpoints |
|---|---|
| Autenticação (`/api/auth`) | `POST /register`, `POST /login` |
| Clientes (`/clientes`) | `POST`, `GET`, `GET /{clienteId}`, `PUT /{clienteId}`, `DELETE /{clienteId}` |
| Veículos (`/veiculos`) | `POST`, `GET`, `PUT /{placa}`, `DELETE /{placa}` |
| Funcionários (`/funcionarios`) | `POST`, `GET`, `GET /{funcionarioId}`, `PUT /{funcionarioId}`, `DELETE /{funcionarioId}` |
| Ordens de Serviço (`/ordens-servico`) | `POST`, `GET`, `GET /{numeroOrdemServico}`, `PUT /{numeroOrdemServico}`, `DELETE /{numeroOrdemServico}`, `GET /metricas/tempo-medio`, `GET /{numeroOrdemServico}/acompanhamento` |
| Diagnóstico da OS | `POST /ordens-servico/{numeroOrdemServico}/diagnostico/iniciar`, `POST /ordens-servico/{numeroOrdemServico}/diagnostico/concluir`, `POST /ordens-servico/{numeroOrdemServico}/diagnostico/enviar-para-orcamento` |
| Execução e entrega da OS | `POST /ordens-servico/{numeroOrdemServico}/execucao/iniciar`, `POST /ordens-servico/{numeroOrdemServico}/servico/concluir`, `POST /ordens-servico/{numeroOrdemServico}/finalizacao`, `POST /ordens-servico/{numeroOrdemServico}/entrega` |
| Orçamentos (`/orcamentos`) | `POST /{orcamentoId}/aprovacao`, `POST /{orcamentoId}/rejeicao`, `GET /{orcamentoId}`, `GET`, `PUT /{orcamentoId}`, `DELETE /{orcamentoId}` |
| Peças e Insumos (`/pecas-insumos`) | `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/adicionar-estoque`, `POST /{id}/remover-estoque`, `POST /{id}/reservar`, `POST /{id}/liberar-reserva`, `POST /{id}/consumir` |
| Webhook de decisão de orçamento (`/integracoes/orcamentos`) | `POST /{numeroOrcamento}/decisao` — exige o header `X-Webhook-Token`, aceitando um token de integração gerado (ver abaixo) ou o segredo estático `ORCAMENTO_WEBHOOK_SECRET` (coexistência temporária) |
| Tokens de integração (`/integracoes/tokens`) | `POST` (gerar), `GET` (listar), `DELETE /{id}` (revogar) — autenticado via JWT, como as demais rotas |

> **Notificações**: não expõem endpoint REST. São disparadas internamente (padrão outbox) a cada mudança de status da OS e reprocessadas por um scheduler em caso de falha de envio (SMTP configurado via `SMTP_HOST`/`SMTP_PORT`).

---

## Estrutura do projeto

```
src/main/java/br/com/oficina/
├── auth/               # JWT stateless — register, login, filtro, SecurityConfig
├── cliente/            # CRUD de clientes (PF/PJ, validacao CPF/CNPJ)
├── veiculo/            # CRUD de veiculos (placa formato antigo e Mercosul)
├── ordemservico/       # Bounded context principal — ciclo de vida da OS
├── orcamento/          # Orcamentos vinculados a OS
├── pecainsumo/         # Estoque de pecas e insumos
├── common/             # Excecoes globais e ApiExceptionHandler
└── config/             # SwaggerConfig, FlywayConfig
```

Arquitetura hexagonal (ports & adapters) com estrutura `domain / application / infrastructure` por módulo.

---

## Infraestrutura e Deploy

### Desenho da arquitetura

```mermaid
flowchart TB
    DEV(["👨‍💻 Dev<br/><i>git push / PR</i>"])
    CLIENT(["🌐 Cliente HTTP<br/><i>Swagger / Insomnia</i>"])

    subgraph GH["⚙️ GitHub Actions"]
        direction LR
        INFRA_WF["🏗️ Infraestrutura<br/><b>manual</b><br/>terraform plan · apply · destroy"]
        CI_WF["🔄 CI/CD<br/><b>push na master</b><br/>testes → imagem → deploy"]
    end

    subgraph AWS["☁️ AWS · us-east-1 · Terraform em /infra/aws"]
        direction TB
        ECR["📦 ECR<br/>registry de imagens"]
        SM["🔐 Secrets Manager<br/>credenciais do RDS"]

        subgraph VPC["🕸️ VPC 10.0.0.0/16 · 2 AZs"]
            direction TB
            NLB["⚖️ NLB público<br/><i>criado pelo Service</i>"]

            subgraph EKS["☸️ EKS · namespace oficina · /k8s"]
                direction TB
                DEP["🚀 Deployment <b>oficina-api</b><br/>2–6 réplicas · probes Actuator"]
                CFG["📄 ConfigMap · 🔑 Secrets"]
                HPA["📈 HPA<br/>CPU 70% · Mem 80%"]
                MS["📊 metrics-server<br/><i>add-on EKS</i>"]
            end

            RDS[("🐘 RDS PostgreSQL 15<br/><i>subnets privadas</i>")]
        end
    end

    DEV --> GH
    INFRA_WF -. "🏗️ provisiona" .-> AWS
    CI_WF -- "docker push" --> ECR
    CI_WF -- "kubectl apply" --> EKS
    CI_WF -- "lê credenciais" --> SM
    CLIENT ==> NLB ==> DEP
    CFG --> DEP
    MS --> HPA
    HPA -. "escala" .-> DEP
    ECR -. "pull via IAM" .-> DEP
    DEP ==> RDS

    classDef pipeline fill:#dbeafe,stroke:#2563eb,stroke-width:2px,color:#1e3a8a
    classDef awsmanaged fill:#ffedd5,stroke:#ea580c,stroke-width:2px,color:#7c2d12
    classDef k8s fill:#e0f2fe,stroke:#0284c7,stroke-width:2px,color:#0c4a6e
    classDef data fill:#dcfce7,stroke:#16a34a,stroke-width:2px,color:#14532d
    classDef actor fill:#f3e8ff,stroke:#9333ea,stroke-width:2px,color:#581c87

    class INFRA_WF,CI_WF pipeline
    class ECR,SM,NLB awsmanaged
    class DEP,CFG,HPA,MS k8s
    class RDS data
    class DEV,CLIENT actor
```

> 🟣 atores · 🔵 pipelines · 🟠 serviços gerenciados AWS · 💧 Kubernetes · 🟢 dados

**Componentes da aplicação:** API Spring Boot (`oficina-api`, arquitetura
hexagonal — ver [Estrutura do projeto](#estrutura-do-projeto)) + PostgreSQL.

**Infraestrutura provisionada (Terraform, [`infra/aws`](infra/README.md)):**
VPC com subnets públicas/privadas/de banco em 2 AZs, cluster **EKS** com
node group gerenciado (2–4× t3.small) e metrics-server, registry **ECR** e
banco **RDS PostgreSQL 15** com credenciais gerenciadas pelo Secrets Manager.

**Fluxo de deploy:** push na `master` → build + testes (unitário e
integração) → imagem Docker publicada no **ECR** (tag = sha do commit) →
job de deploy aponta o `kubectl` para o EKS, materializa o Secret do banco a
partir do Secrets Manager, injeta imagem/endpoint nos manifestos de `/k8s` e
aplica, validando o rollout. A infraestrutura em si é provisionada à parte
pelo workflow **Infraestrutura (Terraform)**, manual, porque criar EKS leva
~15 min.

### Containerização (Docker)

- `Dockerfile`: build multi-stage (Maven → JRE 21 Alpine), usuário não-root,
  `HEALTHCHECK` via `/actuator/health/liveness` (Spring Boot Actuator, público).
- `docker-compose.yml`: sobe banco + aplicação para desenvolvimento local
  (ver [Subindo o ambiente](#subindo-o-ambiente)).

### Kubernetes (`/k8s`)

| Arquivo | Recurso |
|---|---|
| `00-namespace.yaml` | Namespace `oficina` |
| `01-configmap.yaml` | `DB_URL` (placeholder `__RDS_ENDPOINT__`, preenchido pela pipeline com o endpoint do RDS) |
| `02-secret.yaml` | `JWT_SECRET` (valor de demo — ver comentário no arquivo) |
| `03-deployment.yaml` | Deployment da API (placeholder `__IMAGE__` → ECR), 2 réplicas, probes de startup/readiness/liveness via Actuator, requests/limits |
| `04-service.yaml` | Service `LoadBalancer` — a AWS cria um NLB público para a demo |
| `05-hpa.yaml` | HorizontalPodAutoscaler (2-6 réplicas, 70% CPU / 80% memória) |

As credenciais do banco **não estão no repositório**: o RDS as gerencia no
Secrets Manager, e o job de deploy cria o Secret `oficina-db-credentials` no
cluster a cada deploy. Os comandos para aplicar manualmente (substituição dos
placeholders) estão comentados nos próprios manifestos.

### Infraestrutura como código (Terraform, `/infra/aws`)

Recursos criados, custos estimados, bootstrap do state (S3) e instruções de
apply/destroy: **[`infra/README.md`](infra/README.md)**.

```bash
cd infra/aws
terraform init -backend-config=backend.hcl
terraform apply    # VPC + EKS + ECR + RDS (~15-20 min)
```

> ⚠️ Custo: ~US$ 0,27/h com tudo ligado. Aplique quando for usar e destrua
> depois (`terraform destroy` — ver ordem correta no infra/README).

### CI/CD (`.github/workflows/`)

**Secrets/variables do repositório** (Settings → Secrets and variables → Actions):
`AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` (secrets), `TF_STATE_BUCKET` e
opcionalmente `AWS_REGION` (variables).

**`infra.yml` — Infraestrutura (Terraform)**, manual (*Run workflow*):
`plan`, `apply` ou `destroy` da infraestrutura AWS, com state remoto no S3.
No `destroy`, remove antes os recursos do k8s (o NLB é criado fora do
Terraform e travaria a exclusão da VPC).

**`ci-cd.yml` — CI/CD**, em push na `master` e PRs para `master`/`desenvolvimento`:

1. **unit-tests** — `./mvnw clean verify` (build + testes unitários + gate de cobertura JaCoCo).
2. **integration-tests** — `./mvnw test -Dspring.profiles.active=integration` contra um PostgreSQL real (service container).
3. **build-image** — build e push da imagem para o **ECR** (tags: sha do commit e `latest`) — só em push, nunca em PR.
4. **deploy** — `aws eks update-kubeconfig`, cria o Secret do banco a partir do Secrets Manager, injeta imagem/endpoint nos manifestos e `kubectl apply -f k8s/`, aguardando o rollout e imprimindo a URL pública do NLB.

## Variáveis de ambiente

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/oficina_db` |
| `DB_USER` | `oficina_user` |
| `DB_PASS` | `oficina_123` |
| `JWT_SECRET` | chave hardcoded de fallback |
