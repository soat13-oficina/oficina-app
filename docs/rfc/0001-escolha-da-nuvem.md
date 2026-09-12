# RFC 0001 — Escolha da nuvem

- **Status:** Aceito
- **Data:** 2026-09-12
- **Escopo:** Todos os quatro repositórios do projeto

## Contexto

O enunciado da Fase 3 exige API Gateway, Function Serverless, banco gerenciado,
cluster Kubernetes com escalabilidade e Terraform — mas deixa a **escolha da
nuvem livre**. A decisão precisa valer para os quatro repositórios de uma vez:
uma vez tomada, ela se espalha por módulos de Terraform, providers, nomes de
recurso e a forma de autenticar as pipelines.

Duas restrições reais pesaram:

- A Fase 2 já entregou EKS, RDS e ECR na AWS, com Terraform funcionando. Migrar
  significaria reescrever a camada de infraestrutura inteira e perder o que já
  havia sido validado.
- O ambiente é custeado pelo grupo, e o orçamento é o de um projeto acadêmico.

## Proposta

Manter tudo na **AWS**, região `us-east-1`.

| Exigência do enunciado | Serviço |
|---|---|
| API Gateway | Amazon API Gateway HTTP (v2) |
| Function Serverless | AWS Lambda (Node.js 22) |
| Banco gerenciado | Amazon RDS for PostgreSQL 15 |
| Cluster Kubernetes | Amazon EKS |
| Provisionamento | Terraform, providers `hashicorp/aws`, `helm` e `datadog` |

## Alternativas consideradas

| Alternativa | Avaliação |
|---|---|
| **Google Cloud** (GKE, Cloud SQL, Cloud Functions, API Gateway) | Tecnicamente equivalente e com GKE mais simples de operar que o EKS. Descartada pelo custo de migração: reescrever toda a IaC da Fase 2 consumiria a maior parte do tempo desta fase, sem agregar nada ao que é avaliado. |
| **Azure** (AKS, Azure Database for PostgreSQL, Functions, API Management) | Mesma avaliação, com o agravante de que o API Management tem custo de entrada bem mais alto que o HTTP API da AWS. |
| **Multi-cloud** (ex.: cluster na GCP, banco na AWS) | Descartada de imediato. Dobraria o número de credenciais, providers e pontos de falha, e adicionaria latência entre camadas — complexidade sem contrapartida num projeto deste porte. |

## Impacto

**Positivo**

- Reaproveitamento integral da infraestrutura da Fase 2: VPC, EKS, ECR e RDS entraram nos novos repositórios como extração, não como reescrita.
- Os quatro serviços exigidos são de primeira parte e se integram nativamente — a Lambda alcança o RDS pela VPC, o pod assume IAM Role por IRSA, o gateway invoca a função sem *glue code*.
- Provider Terraform maduro, e módulos comunitários (`terraform-aws-modules/vpc` e `eks`) que encurtaram bastante o caminho.

**Negativo e mitigações**

- **Acoplamento ao fornecedor na camada de IaC.** Os módulos são específicos de AWS; trocar de nuvem seria reescrever, não reconfigurar. Aceito conscientemente: a portabilidade real está no que roda **dentro** do cluster — imagem Docker e manifestos Kubernetes são neutros.
- **Custo.** O ambiente completo custa cerca de **US$ 7,40/dia** ligado, com o control plane do EKS (US$ 2,40/dia) como maior item isolado. Mitigado por disciplina de `apply`/`destroy` por sessão, com o `destroy` documentado no roteiro de operação.

## Lições da execução

Duas coisas só apareceram na prática e ficam registradas para quem repetir o
caminho:

**Contas AWS novas entram num plano gratuito que bloqueia EC2 fora do free
tier.** O node group do EKS falha com `InvalidParameterCombination — The
specified instance type is not eligible for Free Tier`, depois de ~30 minutos
girando. Como o control plane do EKS **nunca** foi elegível ao free tier e já é
cobrado desde o primeiro minuto, o plano gratuito não protege nada neste
cenário — só bloqueia. O upgrade para plano pago é pré-requisito, não opção.

**`t3.small` não comporta o stack completo.** Com o DaemonSet do Datadog rodando
em todo nó, sobra pouco para a aplicação. O tipo foi elevado para `t3.medium`,
dobrando o custo dos nós — que ainda assim representam apenas 15% da conta.

## Questões em aberto

- **Credencial de longa duração nas pipelines.** As Actions autenticam por access key com `AdministratorAccess`. O padrão correto é OIDC (GitHub → IAM Role, sem chave estática), descartado nesta fase por ser problema de *bootstrap*: a role que a pipeline usaria seria criada pela própria pipeline.
- **Região única.** Não há estratégia de recuperação entre regiões. Fora do escopo do desafio, mas é a primeira coisa a endereçar num cenário real.
