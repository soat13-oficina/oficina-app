# Referência: cluster e banco em AWS (EKS + RDS)

> Documentação de referência, **não aplicada nem testada** neste repositório.
> Os módulos versionados (`cluster/`, `database/`) provisionam um cluster
> **local** (kind) para que o desafio seja executável sem uma conta cloud.
> Este arquivo mostra o caminho para adaptar a mesma estrutura para AWS.
> Antes de aplicar algo parecido, revise custos (EKS cobra por cluster/hora,
> RDS por instância) e credenciais com o time.

## Ideia geral

Trocar o módulo `infra/cluster` (hoje baseado no provider `kind`) por um
módulo que usa o [`terraform-aws-modules/eks/aws`](https://registry.terraform.io/modules/terraform-aws-modules/eks/aws/latest)
para o cluster, e o `infra/database` (hoje um StatefulSet dentro do cluster)
por um banco gerenciado via [`terraform-aws-modules/rds/aws`](https://registry.terraform.io/modules/terraform-aws-modules/rds/aws/latest).
O restante do fluxo (aplicar `/k8s` com `kubectl`, o Secret de credenciais,
o HPA) não muda.

## Esboço (não aplicado)

```hcl
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = "oficina"
  cluster_version = "1.31"
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnets

  eks_managed_node_groups = {
    default = {
      instance_types = ["t3.medium"]
      min_size       = 2
      max_size       = 4
      desired_size   = 2
    }
  }
}

module "rds" {
  source  = "terraform-aws-modules/rds/aws"
  version = "~> 6.0"

  identifier     = "oficina-db"
  engine         = "postgres"
  engine_version = "15"
  instance_class = "db.t3.micro"
  db_name        = "oficina_db"
  username       = "oficina_user"
  manage_master_user_password = true # senha gerenciada pelo Secrets Manager

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name    = module.vpc.database_subnet_group_name
}
```

Pontos de atenção ao migrar de verdade:

- **Rede**: EKS e RDS precisam de uma VPC com subnets públicas/privadas
  (o kind local não tem esse problema — tudo roda em containers na mesma
  máquina).
- **Credenciais do banco**: em vez do `kubernetes_secret` criado hoje pelo
  módulo `database/`, o ideal é usar o Secrets Manager do RDS (ou o External
  Secrets Operator para sincronizar com um `Secret` do Kubernetes).
- **metrics-server**: em EKS geralmente também precisa ser instalado
  explicitamente (o mesmo `helm_release` do módulo `cluster/` serve, sem o
  `--kubelet-insecure-tls`, que é só workaround do kind).
- **Ingress**: em cloud normalmente se troca o `kubectl port-forward` local
  por um `Service type: LoadBalancer` ou um Ingress Controller (nginx, ALB).
