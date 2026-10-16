# Convenções de nomes — Fase 4

Nomes previsíveis são o que permite a cada serviço achar os recursos dos outros
**sem ler o state Terraform de ninguém** (ex.: o OS descobre a URL da fila do
Billing por `GetQueueUrl` a partir do nome). `<env>` é sempre `hml` ou `prd`.

Nomes **já existentes** na Fase 3 aparecem marcados com *(mantém)*.

## Serviços

| Serviço | Repositório | Nome lógico (`DD_SERVICE`, `spring.application.name`, Deployment, Service, ServiceAccount, Ingress, ECR) | Pacote Java |
|---|---|---|---|
| OS Service | `oficina-app` | `oficina-os-service` (hoje `oficina-api`) | `br.com.oficina` *(mantém)* |
| Billing Service | `oficina-billing-service` | `oficina-billing-service` | `br.com.oficina.billing` |
| Execution Service | `oficina-execution-service` | `oficina-execution-service` | `br.com.oficina.execution` |

`DD_SERVICE` e `spring.application.name` **iguais** (o comentário no `application.yml`
explica: o Datadog correlaciona métricas Micrometer e traces pelo nome).
`DD_ENV` = `hml`/`prd`; `DD_VERSION` = SHA do commit.

## Kubernetes

| Recurso | Nome |
|---|---|
| Namespace | `oficina-hml`, `oficina-prd` *(mantém)* — os três serviços no mesmo namespace |
| Deployment / Service / ServiceAccount | nome lógico do serviço |
| Service (tipo) | `ClusterIP` — o acesso externo é pelo Ingress |
| DNS interno | `http://<nome-logico>:8080` (ex.: `http://oficina-execution-service:8080`) |
| Ingress | nome lógico do serviço, `ingressClassName: alb`, `alb.ingress.kubernetes.io/group.name: oficina-<env>` |
| ConfigMap | `<nome-logico>-config` |
| Secret da aplicação | `<nome-logico>-secrets` |
| Secret do banco | `<nome-logico>-db-credentials` |
| Labels | `app.kubernetes.io/name: <nome-logico>`, `app.kubernetes.io/part-of: oficina`, `tags.datadoghq.com/service: <nome-logico>` |
| Portas | `8080` aplicação, `8081` management/probes *(mantém)* |

## AWS

| Recurso | Nome | Dono (`infra/` de) |
|---|---|---|
| Cluster EKS | `oficina` *(mantém)* | `oficina-infra-k8s` |
| ECR | `oficina-os-service`, `oficina-billing-service`, `oficina-execution-service` | cada serviço |
| Tag de imagem | `<ambiente>-<sha>` e `<ambiente>-latest` *(mantém)* | pipeline |
| RDS do OS | `oficina-db-<env>` *(mantém)* | `oficina-infra-database` |
| RDS do Billing | `oficina-billing-db-<env>` | Billing |
| Filas SQS | `oficina-<env>-billing-commands.fifo`, `oficina-<env>-execution-commands.fifo`, `oficina-<env>-os-saga-replies.fifo` | consumidor da fila |
| DLQs | mesmo nome com `-dlq.fifo` no lugar de `.fifo` | consumidor da fila |
| Tabelas DynamoDB | `oficina-<env>-execucoes`, `oficina-<env>-pecas`, `oficina-<env>-reservas`, `oficina-<env>-execution-controle` | Execution |
| Role IRSA | `oficina-<env>-<nome-logico>` (ex.: `oficina-hml-oficina-billing-service`) | cada serviço |
| Role do controller | `oficina-aws-load-balancer-controller` | `oficina-infra-k8s` |
| ALB | criado pelo controller para o IngressGroup `oficina-<env>` | (Kubernetes) |

## Segredos e parâmetros

| Nome | Conteúdo | Quem cria | Quem lê |
|---|---|---|---|
| `oficina/<env>/jwt-secret` *(mantém)* | Segredo HMAC do JWT | `oficina-lambda-auth` | Lambdas e os três serviços |
| `oficina/<env>/service-token` | Token do header `X-Service-Token` em `/internal/**` | `infra/` do OS | OS, Lambda `token`, Billing, Execution |
| `oficina/<env>/mercadopago` | JSON `{ "access_token", "webhook_secret" }` | `infra/` do Billing (valor colocado à mão) | Billing |
| Segredo gerenciado do RDS | usuário e senha do banco | RDS (`manage_master_user_password`) *(mantém)* | serviço dono do banco |
| SSM `/oficina/<env>/alb-url` | `http://<hostname-do-alb>` | pipeline de deploy | `oficina-lambda-auth` |

## Terraform

| Item | Convenção |
|---|---|
| Workspaces | `hml`, `prd` *(mantém)* |
| Chave do state | `oficina/<repositorio>.tfstate` (ex.: `oficina/billing-service.tfstate`) *(mesmo padrão)* |
| Remote state lido | só o da plataforma (`oficina/infra-k8s.tfstate`) — nunca o de outro serviço |
| Tags | `Project = oficina`, `Environment = <env>`, `Service = <nome-logico>`, `ManagedBy = terraform` |

## Mensageria, métricas e logs

| Item | Convenção |
|---|---|
| `type` das mensagens | PascalCase; comando no imperativo, evento no particípio — ver [`../contratos/mensagens.md`](../contratos/mensagens.md) |
| Métricas | prefixo `oficina.` *(mantém)*: `oficina.saga.*`, `oficina.mensageria.*`, `oficina.outbox.*`, `oficina.pagamento.*`, `oficina.estoque.*` |
| Chaves de MDC | snake_case, como as existentes (`correlation_id`, `numero_ordem_servico`) *(mantém)*; novas: `saga_id`, `ordem_servico_id`, `message_id`, `message_type` |
| Monitores Datadog | `[oficina][<nome-logico>] <descrição>` |

## Git

| Item | Convenção |
|---|---|
| Fluxo de branches | `feature/*` → `desenvolvimento` → `homologacao` → `master` *(mantém, `pr-source-guard.yml`)* |
| Deploy | push em `homologacao` → `oficina-hml`; push em `master` → `oficina-prd` *(mantém)* |
| Commits | Conventional Commits em pt-BR *(mantém)* |
| ADRs | `docs/adr/NNNN-titulo-em-kebab-case.md`; ADRs transversais da Fase 4 vivem no `oficina-app` e os outros repositórios linkam |
