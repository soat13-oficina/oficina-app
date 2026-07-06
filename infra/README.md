# Infraestrutura como Código (Terraform)

Provisiona o **cluster Kubernetes** e o **banco de dados**. Os manifestos da
aplicação em si (Deployment/Service/ConfigMap/Secret/HPA) ficam em [`/k8s`](../k8s)
e são aplicados via `kubectl` **depois** deste Terraform (ver ordem abaixo).

## Módulos

### `cluster/` — cluster Kubernetes local

Cria um cluster [kind](https://kind.sigs.k8s.io/) (Kubernetes rodando em
containers Docker) via o provider [`tehcyx/kind`](https://registry.terraform.io/providers/tehcyx/kind).

Recursos criados:

| Recurso | O que é |
|---|---|
| `kind_cluster.this` | Cluster Kubernetes local (1 control-plane + 1 worker) |
| `local_file.kubeconfig` | Kubeconfig do cluster, salvo em `infra/cluster/kubeconfig` |
| `helm_release.metrics_server` | metrics-server, necessário para o HPA (`k8s/05-hpa.yaml`) conseguir ler CPU/memória dos pods |

### `database/` — banco de dados

Provisiona o PostgreSQL **dentro do cluster** via o provider `kubernetes`
(não depende do módulo `cluster` diretamente — só do kubeconfig que ele gera).

Recursos criados, todos no namespace `oficina`:

| Recurso | O que é |
|---|---|
| `kubernetes_namespace.oficina` | Namespace `oficina`, compartilhado com os manifestos de `/k8s` |
| `kubernetes_secret.db_credentials` | Secret `oficina-db-credentials` (usuário/senha/nome do banco) — também consumido pelo Deployment da API |
| `kubernetes_service.db` | Service headless `oficina-db` (DNS interno `oficina-db.oficina.svc.cluster.local`) |
| `kubernetes_stateful_set.db` | StatefulSet do PostgreSQL com 1 réplica e PVC de dados persistente |

## Pré-requisitos

- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.6
- Docker Desktop (ou outro daemon Docker) rodando — o kind cria os nodes como containers
- `kubectl`

## Como aplicar (cluster local)

```bash
# 1. Cluster Kubernetes + metrics-server
cd infra/cluster
terraform init
terraform apply -auto-approve

# 2. Banco de dados (usa por default o kubeconfig gerado no passo 1)
cd ../database
terraform init
terraform apply -auto-approve

# 3. Manifestos da aplicação (fora do Terraform, ver /k8s)
export KUBECONFIG="$(pwd)/../cluster/kubeconfig"   # PowerShell: $env:KUBECONFIG = "$PWD/../cluster/kubeconfig"
kubectl apply -f ../../k8s/
kubectl -n oficina rollout status deployment/oficina-api
```

A aplicação fica acessível localmente via:

```bash
kubectl -n oficina port-forward svc/oficina-api 8080:80
```

## Apontando para um cluster cloud em vez do kind local

O módulo `database/` não tem nenhuma dependência do kind — ele só precisa de
um kubeconfig válido. Para usar um cluster gerenciado (EKS, GKE, AKS, etc.):

1. Gere o kubeconfig do cluster cloud (ex.: `aws eks update-kubeconfig --name <cluster>`).
2. Pule o módulo `cluster/` (ou adapte-o para provisionar o cluster gerenciado — ver
   [`cloud-aws-reference.md`](cloud-aws-reference.md) para um ponto de partida com EKS/RDS).
3. Rode `terraform apply -var="kubeconfig_path=/caminho/para/seu/kubeconfig"` em `database/`.
4. Aplique `/k8s` normalmente.

## Destruir

```bash
cd infra/database && terraform destroy -auto-approve
cd ../cluster && terraform destroy -auto-approve
```

## Limitações conhecidas (escopo de desafio técnico, não produção)

- **State local**: cada módulo guarda o `.tfstate` em disco (gitignored). Para um
  time real, use um backend remoto (S3+DynamoDB, Terraform Cloud, etc.).
- **Senha do banco com default**: `db_password` tem um valor default só para
  facilitar a demo (mesmo espírito do `docker-compose.yml`). Sobrescreva via
  `-var`/`TF_VAR_db_password` em qualquer uso além de demo local/CI.
- **Cloud (EKS/RDS) não está implementado/aplicado** — apenas documentado como
  referência, para não versionar um recurso pago não testado.
