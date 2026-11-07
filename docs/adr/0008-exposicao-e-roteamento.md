# ADR 0008 — Exposição dos serviços: AWS Load Balancer Controller e um ALB por ambiente

- **Status:** Proposto
- **Data:** 2026-10-15
- **Contexto:** Tech Challenge SOAT 13 — Fase 4

## Contexto

Na Fase 3 a aplicação é exposta por um Service `LoadBalancer`, que cria **um NLB
público por ambiente**; o API Gateway (`oficina-lambda-auth`) encaminha
`ANY /{proxy+}` para o hostname desse NLB, que fica **fixo** em `variables.tf` —
configuração manual a cada recriação do ambiente.

Com três serviços ([ADR 0003](0003-divisao-em-microsservicos-e-dono-dos-dados.md)),
manter o modelo significa **seis NLBs** (3 serviços × 2 ambientes), seis hostnames
para copiar à mão e roteamento por prefixo no API Gateway, uma integração por
serviço. Além disso:

- o webhook do Mercado Pago precisa de uma rota **pública, sem authorizer** ([ADR 0007](0007-mercado-pago-checkout-pro.md));
- a Lambda `token` passa a chamar o OS por HTTP em `/internal/clientes/por-cpf/{cpf}`, que **não** pode virar rota pública;
- o controlador de Ingress mais usado, o **ingress-nginx, foi aposentado** pelo projeto Kubernetes: anúncio em 11/11/2025, manutenção encerrada em março de 2026, sem novas versões nem correções de segurança.

## Decisão

**AWS Load Balancer Controller no cluster e um ALB por ambiente, compartilhado
pelos três serviços via IngressGroup.**

- O controller é instalado por Helm no `oficina-infra-k8s`, com role IRSA própria (política IAM oficial do projeto, versão fixada).
- Cada serviço publica um `Ingress` (`ingressClassName: alb`) com a anotação `alb.ingress.kubernetes.io/group.name: oficina-<env>`; o controller junta os três num **único ALB** por ambiente.
- `target-type: ip` (o ALB fala direto com os pods); health check em `/actuator/health/readiness` na porta de management `8081`, a mesma das probes atuais.
- Os Services passam de `LoadBalancer` para `ClusterIP`.
- ALB **internet-facing**, como o NLB de hoje: o API Gateway HTTP continua usando integração `HTTP_PROXY` por hostname.

### Roteamento por caminho

| Caminho | Serviço | Rota no API Gateway |
|---|---|---|
| `POST /auth` | Lambda `token` | Pública |
| `/api/auth/**`, `/clientes/**`, `/veiculos/**`, `/funcionarios/**`, `/ordens-servico/**`, `/sagas/**` | OS | `ANY /{proxy+}` com authorizer (como hoje) |
| `/orcamentos/**`, `/integracoes/**`, `/pagamentos/**` | Billing | `ANY /{proxy+}` com authorizer |
| `POST /webhooks/mercadopago` | Billing | **Pública**, sem authorizer — protegida pela assinatura `x-signature` |
| `/execucoes/**`, `/pecas-insumos/**` | Execution | `ANY /{proxy+}` com authorizer |
| `/internal/clientes/**` | OS | **Nenhuma** — chamado pela Lambda direto no ALB |
| `/internal/pecas-insumos/**` | Execution | **Nenhuma, e fora de qualquer Ingress** — o Billing chama pelo DNS do cluster (`http://oficina-execution-service:8080`) |

### Caminhos internos

`/internal/**` exige o header `X-Service-Token` (segredo `oficina/<env>/service-token`,
comparação em tempo constante) **na própria aplicação**, que é o controle que vale
em qualquer caminho de rede. O único caminho interno exposto no ALB é o que a
Lambda precisa (`/internal/clientes/**`); o do Execution nem entra no Ingress.

### Descoberta do endereço, sem edição manual

1. O pipeline de deploy de cada serviço, depois do rollout, lê o hostname do ALB no `status` do Ingress e grava no SSM Parameter Store em `/oficina/<env>/alb-url`.
2. O Terraform do `oficina-lambda-auth` lê esse parâmetro (`data "aws_ssm_parameter"`) e monta a integração `ANY /{proxy+}` e a da Lambda `token` — some o `app_base_url` fixo em `variables.tf`.

Ordem de provisionamento: `infra-k8s` (controller) → `infra-database` → `infra/`
dos serviços → deploy dos serviços (o ALB nasce no primeiro Ingress) →
`lambda-auth`. Destruição na ordem inversa, **apagando os Ingresses antes** do
cluster: ALB e security groups criados pelo controller não são do Terraform e
impedem a exclusão da VPC se ficarem órfãos.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| **Um NLB por serviço** e roteamento por prefixo no API Gateway | Funciona sem componente novo — é o **plano B** se o controller atrasar. Mas são 6 NLBs, 6 hostnames e 6 integrações para manter, e o custo cresce com cada serviço. |
| **ingress-nginx** atrás de um NLB | Projeto aposentado (sem correções de segurança desde março de 2026). Começar um serviço novo sobre ele seria dívida técnica nascendo. |
| **Outro controlador no cluster** (Traefik, Envoy Gateway) | Mais um componente para operar dentro de um cluster já apertado, e ainda precisaria de um NLB na frente. O controller da AWS cria o balanceador gerenciado e não fica no caminho do tráfego. |
| **ALB interno + VPC Link** no API Gateway | Mais seguro (nenhum tráfego chega ao ALB sem passar pelo gateway) e a Lambda, já na VPC, chegaria ao `/internal` sem exposição. Exige descobrir o listener do ALB criado pelo controller e configurar o VPC Link no Terraform. Fica como **evolução** — hoje cada serviço já valida o JWT por conta própria, então o acesso direto ao ALB não contorna a autenticação. |
| **API Gateway por serviço** | Multiplicaria URLs públicas e authorizers sem necessidade. |

## Consequências

**Positivas**

- Um ALB por ambiente para todos os serviços; acrescentar um serviço é acrescentar um Ingress.
- Roteamento L7 por caminho no ALB, em vez de uma integração por serviço no gateway.
- Fim da configuração manual do hostname: o SSM liga o deploy ao gateway.
- Substitui um padrão em fim de vida por um mantido pela AWS.

**Negativas e mitigações**

- **Mais um componente no `oficina-infra-k8s`** (Helm + IRSA + política IAM). Versão fixada e documentada no README.
- **Recursos fora do Terraform** (ALB, target groups, security groups). Mitigado pela ordem de destruição acima e por um passo no workflow de destroy que remove os Ingresses antes.
- **ALB público aceita acesso direto**, sem o authorizer. Os serviços validam JWT e `X-Service-Token`; a evolução para ALB interno + VPC Link está registrada.
- **Custo:** um ALB custa por hora como o NLB que substitui; com três serviços, sai mais barato que o plano B.

## Relacionado

- [ADR 0003](0003-divisao-em-microsservicos-e-dono-dos-dados.md) — chamadas síncronas internas
- [ADR 0007](0007-mercado-pago-checkout-pro.md) — webhook público
- [`../arquitetura/convencoes-de-nomes.md`](../arquitetura/convencoes-de-nomes.md) — nomes de Ingress, Services e parâmetros SSM
- [Ingress NGINX Retirement: What You Need to Know](https://kubernetes.io/blog/2025/11/11/ingress-nginx-retirement/) — anúncio oficial do Kubernetes
