# infra/ — OS Service

Terraform do que é **só deste serviço** (Fase 4, "cada serviço com sua infraestrutura" —
[`PLANO-FASE-4.md`](../docs/PLANO-FASE-4.md), seção 9.1). A plataforma (VPC, EKS, ECR) continua em `oficina-infra-k8s` e o
RDS do OS em `oficina-infra-database`.

| Recurso | Nome | Para quê |
|---|---|---|
| Fila SQS FIFO + DLQ | `oficina-<env>-os-saga-replies.fifo` / `-dlq.fifo` | Respostas e eventos dos participantes da Saga ([ADR 0005](../docs/adr/0005-sqs-fifo-outbox-e-idempotencia.md)). Cada serviço cria a fila que **consome**; quem produz só precisa do nome |
| IAM Role (IRSA) | `oficina-<env>-oficina-os-service` | Pod do OS: consumir `os-saga-replies`, mandar para a própria DLQ, enviar para `billing-commands` e `execution-commands`, `ses:SendEmail`. Confia só na ServiceAccount `oficina-api` do namespace `oficina-<env>` |
| Segredo | `oficina/<env>/service-token` | `X-Service-Token` das rotas `/internal/**`; lido pelas pipelines do OS e do Billing |

A fila `os-saga-replies`: visibilidade de 60 s, long polling de 20 s, 5 recebimentos antes da DLQ,
criptografia gerenciada pelo SQS. As filas dos outros serviços entram na policy pelo ARN montado
com o nome (sem ler o state deles).

## Como aplicar

**Só manualmente** — workflow **Infra (OS Service)** (`.github/workflows/infra.yml`) →
*Run workflow* → ambiente e ação (`plan`, `apply`, `destroy`). Em PR e push o workflow só roda
`fmt` e `validate`, sem AWS.

Ordem (seção 9.2 do plano): `oficina-infra-k8s` → `oficina-infra-database` → **este** → `infra/` do
Billing → `infra/` da Execution → deploy dos serviços → `oficina-lambda-auth`. O destroy é a ordem
inversa.

Localmente:

```sh
cd infra
cp backend.hcl.example backend.hcl            # bucket dos states (nao versionado)
cp terraform.tfvars.example terraform.tfvars  # a mesma bucket, para o remote state da plataforma
terraform init -backend-config=backend.hcl
terraform workspace select -or-create hml
terraform plan
```

O workspace `default` é recusado de propósito (`locals.tf`).

## Contratos com outros repositórios

- **Pipeline deste repo** (`ci-cd.yml`): descobre a Role por `aws iam get-role --role-name
  oficina-<env>-oficina-os-service` e lê `oficina/<env>/service-token`; sem eles o deploy falha
  pedindo o apply deste `infra/`.
- **Aplicação**: `mensageria.sqs.ambiente` (`AMBIENTE`) compõe o nome físico das filas; o envelope e
  os atributos estão em `docs/arquitetura/mensageria-local.md`.
- **Billing** (`oficina-billing-service/infra`): cria `billing-commands` e lê o mesmo
  `service-token`.
- **Plataforma** (`oficina-infra-k8s`): output `oidc_provider_arn`. A role
  `<cluster>-ses-send-email` de lá deixa de ser usada pelo OS e pode sair num próximo PR daquele
  repositório.

O ECR do OS continua em `oficina-infra-k8s` até o rename para `oficina-os-service`.
