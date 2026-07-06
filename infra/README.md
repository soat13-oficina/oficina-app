# Infraestrutura como Código (Terraform — AWS)

Provisiona toda a infraestrutura na AWS: **rede (VPC)**, **cluster Kubernetes
(EKS)**, **registry de imagens (ECR)** e **banco de dados (RDS PostgreSQL)**.
Os manifestos da aplicação ficam em [`/k8s`](../k8s) e são aplicados pela
pipeline de CI/CD (ou manualmente via `kubectl`) **depois** deste Terraform.

## Recursos criados (`infra/aws`)

| Arquivo | Recursos | O que é |
|---|---|---|
| `vpc.tf` | `module.vpc` | VPC `10.0.0.0/16` em 2 AZs: subnets públicas (LoadBalancers), privadas (nodes do EKS) e de banco (RDS), com 1 NAT Gateway |
| `eks.tf` | `module.eks` | Cluster EKS (Kubernetes gerenciado) + node group (2–4× `t3.medium`) + add-ons `coredns`, `kube-proxy`, `vpc-cni` e **`metrics-server`** (alimenta o HPA) |
| `ecr.tf` | `aws_ecr_repository` | Repositório de imagens Docker `oficina`, com scan on push e política de reter só as 10 últimas imagens |
| `rds.tf` | `aws_db_instance` + SG | PostgreSQL 15 gerenciado (`db.t3.micro`, 20 GiB), acessível **apenas** a partir dos nodes do EKS; senha gerada e guardada pela AWS no Secrets Manager |

A senha do banco **não passa pelo state do Terraform** nem pelo repositório:
o RDS gerencia usuário/senha no Secrets Manager (`manage_master_user_password`),
e a pipeline lê de lá para materializar o Secret `oficina-db-credentials` no
cluster na hora do deploy.

## Custo estimado (us-east-1, região default)

| Recurso | ~US$/hora |
|---|---|
| EKS control plane | 0,10 |
| 2× t3.medium | 0,083 |
| NAT Gateway | 0,045 |
| RDS db.t3.micro | 0,017 |
| NLB (criado pelo Service da API) | 0,0225 |
| **Total ligado** | **≈ 0,27/h (~US$ 6,50/dia)** |

**Não deixe ligado sem uso.** O fluxo saudável é `apply` para testar/gravar o
vídeo e `destroy` depois. Versões antigas de Kubernetes caem em *extended
support* e o control plane passa a custar 6x — por isso `kubernetes_version`
default é 1.34 (standard support).

## Pré-requisitos

- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.10
- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) configurada (`aws configure`) com uma credencial com permissão de admin na conta
- `kubectl`

## Bootstrap (uma única vez por conta)

O state fica num bucket S3 compartilhado pelo grupo e pela pipeline:

```bash
aws s3api create-bucket --bucket SEU-BUCKET-DE-STATE --region us-east-1
aws s3api put-bucket-versioning --bucket SEU-BUCKET-DE-STATE \
  --versioning-configuration Status=Enabled
```

Depois, em `infra/aws`, copie `backend.hcl.example` para `backend.hcl`
(gitignored) e preencha o nome do bucket.

## Como aplicar

**Opção A — pela pipeline (recomendado):** GitHub → aba *Actions* → workflow
**Infraestrutura (Terraform)** → *Run workflow* → action `apply`. Requer os
secrets `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` e a variable
`TF_STATE_BUCKET` configurados no repositório.

**Opção B — local:**

```bash
cd infra/aws
terraform init -backend-config=backend.hcl
terraform apply          # ~15-20 min na primeira vez (EKS demora)
```

Ao final, os outputs mostram o comando de acesso e os endpoints:

```bash
terraform output
# configure_kubectl     = "aws eks update-kubeconfig --region us-east-1 --name oficina"
# ecr_repository_url    = "<conta>.dkr.ecr.us-east-1.amazonaws.com/oficina"
# rds_endpoint          = "oficina-db.xxxx.us-east-1.rds.amazonaws.com:5432"
# rds_master_secret_arn = "arn:aws:secretsmanager:..."
```

O deploy da aplicação em si acontece pela pipeline de CI/CD a cada push na
`master` (ver [README principal](../README.md#infraestrutura-e-deploy)). Para
aplicar os manifestos manualmente, os comandos de substituição dos
placeholders estão comentados em `k8s/01-configmap.yaml` e
`k8s/03-deployment.yaml`.

## Destruir

⚠️ **Ordem importa:** o NLB do Service `oficina-api` é criado pelo
Kubernetes, fora do Terraform. Se ele existir na hora do `destroy`, a VPC
trava com dependências órfãs.

```bash
aws eks update-kubeconfig --region us-east-1 --name oficina
kubectl delete -f ../../k8s/ --ignore-not-found   # remove o NLB junto
cd infra/aws && terraform destroy
```

O workflow **Infraestrutura (Terraform)** com action `destroy` já faz essa
limpeza automaticamente antes do destroy.

## Limitações conhecidas (escopo de desafio técnico, não produção)

- **NAT único** e **RDS single-AZ sem backup automático**: barato e suficiente
  para demo; produção pediria NAT por AZ, multi-AZ e `backup_retention_period >= 7`.
- **Credencial da pipeline via access keys**: para um time real, o padrão
  melhor é OIDC (GitHub → IAM role, sem chave de longa duração).
- **RDS acessível só de dentro da VPC**: para inspecionar o banco localmente,
  use `kubectl port-forward` num pod utilitário ou um bastion temporário.
