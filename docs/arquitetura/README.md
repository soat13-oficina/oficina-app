# Documentação da Arquitetura

Ponto de entrada da documentação arquitetural do Tech Challenge SOAT 13 — Fase 3.

O sistema está distribuído em quatro repositórios, e a documentação segue a mesma
divisão: decisões que pertencem a uma camada moram no repositório dela; o que
atravessa o sistema inteiro mora aqui.

---

## Mapa das entregas

| Exigência do enunciado | Onde está |
|---|---|
| **Diagrama de Componentes** — visão de nuvem, APIs, banco e monitoramento | [`diagrama-componentes.md`](diagrama-componentes.md) |
| **Diagrama de Sequência** — autenticação e abertura de ordem de serviço | [`diagramas-sequencia.md`](diagramas-sequencia.md) |
| **RFCs** — decisões técnicas relevantes | [`../rfc/`](../rfc/) — nuvem, banco e autenticação |
| **ADRs** — decisões arquiteturais permanentes | [`../adr/`](../adr/) e os repositórios de infraestrutura (tabela abaixo) |
| **Justificativa do banco + modelo ER + relacionamentos** | [`modelo-de-dados.md`](modelo-de-dados.md) e [RFC 0002](../rfc/0002-escolha-do-banco-de-dados.md) |

---

## RFCs — decisões técnicas

Registram **como se chegou** a uma escolha: contexto, alternativas avaliadas,
impacto e o que ficou em aberto.

| # | Decisão | Resumo |
|---|---|---|
| [0001](../rfc/0001-escolha-da-nuvem.md) | Escolha da nuvem | AWS, por reaproveitamento da Fase 2 e por todos os serviços exigidos serem de primeira parte. Inclui as armadilhas encontradas na execução |
| [0002](../rfc/0002-escolha-do-banco-de-dados.md) | Escolha do banco de dados | PostgreSQL 15 no RDS: domínio fortemente relacional, transação na reserva de estoque e índice funcional |
| [0003](../rfc/0003-estrategia-de-autenticacao.md) | Estratégia de autenticação | JWT HMAC com Lambda Authorizer, convivendo com o login por e-mail e senha já existente |

## ADRs — decisões permanentes

Registram **o que vale** e por quê. Estão distribuídos pelos repositórios, junto
do código que governam.

| # | Decisão | Repositório |
|---|---|---|
| [0001](../adr/0001-hpa-e-estrategia-de-escala.md) | HPA e node group — escala em dois níveis | `oficina-app` |
| [0002](../adr/0002-snapshot-em-ordens-e-orcamentos.md) | Snapshot de dados em OS e orçamentos | `oficina-app` |
| [0001](https://github.com/soat13-oficina/oficina-infra-k8s/blob/master/docs/adr/0001-separacao-dos-states.md) | Separação dos states do Terraform | `oficina-infra-k8s` |
| [0002](https://github.com/soat13-oficina/oficina-infra-k8s/blob/master/docs/adr/0002-plataforma-compartilhada.md) | Plataforma compartilhada, ambientes por namespace | `oficina-infra-k8s` |
| [0003](https://github.com/soat13-oficina/oficina-infra-k8s/blob/master/docs/adr/0003-observabilidade-datadog.md) | Observabilidade com Datadog | `oficina-infra-k8s` |
| [0001](https://github.com/soat13-oficina/oficina-infra-database/blob/master/docs/adr/0001-consumo-do-state-da-plataforma.md) | Consumo do state da plataforma e workspaces | `oficina-infra-database` |
| [0001](https://github.com/soat13-oficina/oficina-lambda-auth/blob/master/docs/adr/0001-api-gateway-e-autorizacao.md) | API Gateway HTTP, Lambda Authorizer e JWT HS256 | `oficina-lambda-auth` |

**Por que distribuídos.** Um ADR vive perto do código que ele governa: quem abre
`oficina-infra-database` para mexer no RDS encontra, no mesmo repositório, a
razão de o state ser lido da plataforma. Centralizar tudo aqui criaria uma
documentação que envelhece separada do código — exatamente o que ADR existe para
evitar.

---

## Diferença entre RFC e ADR neste projeto

Os dois termos costumam ser usados de forma intercambiável. Aqui têm papéis
distintos:

- **RFC** documenta uma **escolha entre alternativas**, com o processo de
  avaliação. Responde *"por que AWS e não GCP?"*. É mais longo, tem seção de
  alternativas descartadas e questões em aberto.
- **ADR** documenta uma **decisão já tomada que rege o código**, com suas
  consequências. Responde *"por que esta tabela duplica o nome do cliente?"*. É
  mais curto e mais próximo da implementação.

Na prática: RFC vem antes e explica a direção; ADR vem junto do código e explica
a regra.

---

## Documentação relacionada

| Documento | Conteúdo |
|---|---|
| [`../fluxograma.md`](../fluxograma.md) | Visão **interna** da aplicação: arquitetura hexagonal, ciclo de vida da OS e do orçamento |
| [`../observabilidade.md`](../observabilidade.md) | Métricas de negócio, logs estruturados, correlação e como conferir localmente |
| [`../teste-funcional.md`](../teste-funcional.md) | Roteiro de teste funcional da API |
| [`../../README.md`](../../README.md) | Execução local, deploy e configuração |

## Como visualizar os diagramas

Todos os diagramas são **Mermaid** em blocos de código, renderizados
automaticamente pelo GitHub ao abrir o arquivo. Para editar localmente, a
extensão *Markdown Preview Mermaid Support* do VS Code mostra o resultado ao
lado do texto.
