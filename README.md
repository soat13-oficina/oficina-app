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

### Testes unitários e de controller

```bash
./mvnw test
```

Utilizam **H2 in-memory** — não é necessário banco externo. Cobertura mínima de 80% (linhas e branches) é verificada pelo JaCoCo. O build falha se a cobertura não for atingida.

### Testes de integração (PostgreSQL real)

Exigem o banco rodando (`docker-compose up -d db`).

```bash
./mvnw test -Dspring.profiles.active=integration
```

Os testes de integração validam os repositórios JPA contra o PostgreSQL real e usam `@Transactional` com rollback automático — nenhum dado persiste entre testes.

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
    subgraph GH["GitHub Actions"]
        direction LR
        INFRA_WF["Infraestrutura (manual)\nterraform plan/apply/destroy"]
        CI_WF["CI/CD (push na master)\nbuild -> testes -> imagem -> deploy"]
    end

    subgraph AWS["AWS (us-east-1) - Terraform em /infra/aws"]
        ECR["ECR\nregistry de imagens"]
        SM["Secrets Manager\ncredenciais do RDS"]

        subgraph VPC["VPC 10.0.0.0/16 (2 AZs)"]
            NLB["NLB publico\n(criado pelo Service)"]

            subgraph EKS["EKS - namespace oficina (/k8s)"]
                CM["ConfigMap"]
                SEC["Secrets"]
                DEP["Deployment oficina-api\n2-6 replicas"]
                HPA["HPA\nCPU 70% / Mem 80%"]
                MS["metrics-server\n(add-on EKS)"]
            end

            RDS[("RDS PostgreSQL 15\nsubnets privadas")]
        end
    end

    CLIENT([Cliente HTTP]) --> NLB --> DEP
    INFRA_WF -- provisiona --> AWS
    CI_WF -- docker push --> ECR
    CI_WF -- kubectl apply --> EKS
    CI_WF -- le credenciais --> SM
    CM --> DEP
    SEC --> DEP
    MS --> HPA
    HPA -. escala .-> DEP
    DEP --> RDS
    ECR -. pull via IAM .-> DEP
```

**Componentes da aplicação:** API Spring Boot (`oficina-api`, arquitetura
hexagonal — ver [Estrutura do projeto](#estrutura-do-projeto)) + PostgreSQL.

**Infraestrutura provisionada (Terraform, [`infra/aws`](infra/README.md)):**
VPC com subnets públicas/privadas/de banco em 2 AZs, cluster **EKS** com
node group gerenciado (2–4× t3.medium) e metrics-server, registry **ECR** e
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
  `HEALTHCHECK` via `/v3/api-docs` (endpoint público do Swagger, não exige JWT).
- `docker-compose.yml`: sobe banco + aplicação para desenvolvimento local
  (ver [Subindo o ambiente](#subindo-o-ambiente)).

### Kubernetes (`/k8s`)

| Arquivo | Recurso |
|---|---|
| `00-namespace.yaml` | Namespace `oficina` |
| `01-configmap.yaml` | `DB_URL` (placeholder `__RDS_ENDPOINT__`, preenchido pela pipeline com o endpoint do RDS) |
| `02-secret.yaml` | `JWT_SECRET` (valor de demo — ver comentário no arquivo) |
| `03-deployment.yaml` | Deployment da API (placeholder `__IMAGE__` → ECR), 2 réplicas, probes, requests/limits |
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

**`ci-cd.yml` — CI/CD**, a cada push/PR na `master`:

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
