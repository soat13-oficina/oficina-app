# Oficina API — Tech Challenge Fase 2 (14SOAT)

Backend monolítico para gestão de oficina mecânica. **Java 21 · Spring Boot 4 · PostgreSQL · Arquitetura Hexagonal.**

> **Para agentes LLM**: o índice de navegação técnica detalhado vive em
> [`docs/contexto-tecnico.md`](docs/contexto-tecnico.md) e nos READMEs de cada domínio
> (`src/main/java/br/com/oficina/<dominio>/README.md`). Leia apenas o que for relevante à tarefa.

---

## Descrição da solução e objetivos da Fase 2

A aplicação gerencia o ciclo de vida de **ordens de serviço (OS)** de uma oficina — do cadastro de
clientes, veículos e peças à abertura da OS, diagnóstico, orçamento, execução, finalização e entrega —
com notificação ao cliente a cada mudança de status.

A **Fase 2** evolui o sistema da Fase 1 para **qualidade, resiliência e escalabilidade**:

- **Qualidade e organização do código**: Clean Code + arquitetura hexagonal (domínio isolado de
  framework/infra), com testes automatizados cobrindo os fluxos críticos.
- **APIs do desafio**: abertura de OS, consulta de status, decisão externa de orçamento (webhook),
  listagem priorizada e notificação de status por e-mail.
- **Infraestrutura escalável** (containerização, orquestração Kubernetes com autoescala, IaC e CI/CD)
  — ver [estado de conformidade](#estado-de-conformidade-fase-2): parte entregue, parte planejada.

### Estado de conformidade (Fase 2)

O mapa completo (requisito → conforme/parcial/ausente → evidência) está em
**[`docs/conformidade-fase2.md`](docs/conformidade-fase2.md)**. Resumo:

| Bloco | Estado |
|---|---|
| Evolução da aplicação (APIs, Clean Code, testes) | ✅ Conforme (arquitetura hexagonal 🟡 parcial — dívida de JPA no domínio em módulos legados) |
| Containerização (Docker + docker-compose) | ✅ Conforme |
| Kubernetes (manifestos + HPA) | ❌ Planejado — `/k8s` (feature futura) |
| Terraform (IaC) | ❌ Planejado — `/infra` (feature futura) |
| CI/CD (pipeline) | ❌ Planejado — GitHub Actions (feature futura) |
| README + collection + vídeo | ✅ Conforme (link do vídeo a publicar) |

---

## Arquitetura

### Componentes da aplicação

```
                         ┌─────────────────────────────────────────────┐
   Cliente HTTP  ─────▶  │  Oficina API (Spring Boot, monólito hexagonal) │
   (JWT Bearer)          │                                               │
                         │  auth · cliente · veiculo · ordemservico ·    │
   Webhook externo ────▶ │  orcamento · pecainsumo · notificacao         │
   (X-Webhook-Token)     │                                               │
                         │  evento StatusOrdemDeServicoAlterado          │
                         │        └─▶ notificacao (outbox) ─▶ SMTP       │
                         └───────────────────────┬───────────────────────┘
                                                 │ JPA / Flyway
                                                 ▼
                                         ┌────────────────┐
                                         │  PostgreSQL    │
                                         └────────────────┘
```

Cada módulo segue camadas `domain` (modelo + ports) · `application` (use cases/services + command/query)
· `infrastructure` (persistência JPA + web). Detalhes em [`docs/contexto-tecnico.md`](docs/contexto-tecnico.md).

### Infraestrutura proposta 🟡 *planejada (features de acompanhamento)*

```
   GitHub Actions (CI/CD)                Terraform (IaC)
   build → test → imagem → deploy        provisiona cluster local + banco
              │                                   │
              ▼                                   ▼
        ┌──────────────────────── Kubernetes (kind/minikube) ────────────────────────┐
        │  Deployment(api) ─ Service ─ ConfigMap ─ Secret(JWT/webhook) ─ HPA(CPU/mem) │
        │  Deployment/StatefulSet(PostgreSQL) ─ Service                               │
        └────────────────────────────────────────────────────────────────────────────┘
```

> Alvos decididos (spec 009): **cluster Kubernetes local (kind/minikube)** + banco via **Terraform**,
> **pipeline GitHub Actions**, com meta de **deploy real demonstrável** (autoescala exibida no vídeo).

### Fluxo de deploy (alvo)

`push` → CI (build + testes + JaCoCo) → build da imagem Docker → `terraform apply` (cluster + banco) →
`kubectl apply` dos manifestos (`/k8s`) → aplicação no ar com HPA escalando por consumo.

---

## Como executar

### Pré-requisitos

Java 21+ · Docker · Maven Wrapper (`./mvnw`).

### Opção A — Stack completa via Docker Compose

```bash
docker-compose up --build
```

- API em **http://localhost:8081** · Swagger em **http://localhost:8081/swagger-ui/index.html**
- PostgreSQL publicado em `localhost:5433`.

### Opção B — App local (Maven) + banco em container

```bash
docker-compose up -d db        # PostgreSQL publicado em localhost:5433
DB_URL=jdbc:postgresql://localhost:5433/oficina_db ./mvnw spring-boot:run
```

- API em **http://localhost:8080** · Swagger em **http://localhost:8080/swagger-ui/index.html**

> Autenticação no Swagger: `POST /api/auth/register` → copie o token → botão **Authorize** (sem `Bearer`).

### Deploy em Kubernetes 🟡 *roadmap*

Os manifestos ficarão em `/k8s` (Deployments, Services, ConfigMaps/Secrets, HPA). Aplicação prevista:
`kubectl apply -f k8s/`. Pendente de feature de acompanhamento.

### Provisionamento com Terraform 🟡 *roadmap*

Os scripts ficarão em `/infra` (cluster local + banco). Aplicação prevista: `terraform init && terraform apply`.
Pendente de feature de acompanhamento.

### Testes

```bash
docker-compose up -d db                 # necessário para os testes de integração (perfil PostgreSQL em :5433)
./mvnw verify                           # unitários + web (H2) + integração (PostgreSQL) + JaCoCo
```

Relatório de cobertura: `target/site/jacoco/index.html`. Cobertura mínima exigida: 80% por módulo de negócio.

---

## Endpoints REST

Todas as rotas exigem `Authorization: Bearer <token>`, exceto: `POST /api/auth/**`, Swagger,
o webhook de decisão (`X-Webhook-Token`) e o acompanhamento público de OS.

| Domínio | Endpoints |
|---|---|
| `auth` | `POST /api/auth/login` · `POST /api/auth/register` *(públicos)* |
| `cliente` | `GET /clientes` (`?termo=`) · `POST /clientes` · `GET/PUT/DELETE /clientes/{id}` |
| `veiculo` | `GET /veiculos` (filtros) · `POST /veiculos` · `PUT/DELETE /veiculos/{placa}` |
| `funcionario` | `GET /funcionarios` · `POST /funcionarios` · `GET/PUT/DELETE /funcionarios/{funcionarioId}` |
| `ordemservico` | `POST /ordens-servico` · `GET /ordens-servico` (filtros, listagem priorizada) · `GET/PUT/DELETE /ordens-servico/{numero}` · `GET /ordens-servico/{numero}/acompanhamento` · `GET /ordens-servico/metricas/tempo-medio` · transições: `…/diagnostico/iniciar`, `…/diagnostico/concluir`, `…/diagnostico/enviar-para-orcamento`, `…/orcamento/enviar-aprovacao`, `…/execucao/iniciar`, `…/servico/concluir`, `…/finalizacao`, `…/entrega` |
| `orcamento` | `GET /orcamentos/{numero}` · `POST /integracoes/orcamentos/{numero}/decisao` *(`X-Webhook-Token`)* |
| `pecainsumo` | `GET /pecas-insumos` · `POST /pecas-insumos` · `GET/PUT/DELETE /pecas-insumos/{id}` |

Coleção completa para testes: **[`docs/collections/oficina-api.insomnia.json`](docs/collections/oficina-api.insomnia.json)**
(variáveis: `base_url`, `token`, `webhook_token`, `cliente_id`, `funcionario_id`, `veiculo_placa`, `numero_os`, `numero_orcamento`).

---

## Ciclo de vida da OS

`Recebida → Diagnóstico → Aguardando Aprovação → Execução → Finalizada → Entregue`

A decisão de orçamento chega por webhook externo (aprovação → Execução; recusa → Finalizada com
`motivoEncerramento=ORCAMENTO_RECUSADO`). A cada transição, um evento dispara a notificação por e-mail
ao cliente (entrega garantida via outbox + reprocessamento agendado; falha de SMTP não causa rollback).

---

## Vídeo demonstrativo

🟡 *A publicar (YouTube/Vimeo, até 15 min)*: `LINK_DO_VIDEO`. Deve demonstrar deploy, execução do CI/CD,
consumo das APIs e escalabilidade automática.

---

## Variáveis de ambiente

| Variável | Padrão (dev) | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/oficina_db` | Conexão do banco (use `:5433` com o compose) |
| `DB_USER` / `DB_PASS` | `oficina_user` / `oficina_123` | Credenciais do banco |
| `JWT_SECRET` | fallback de desenvolvimento | Assinatura JWT (token expira em 24h) |
| `ORCAMENTO_WEBHOOK_SECRET` | `segredo-desenvolvimento` | Token do webhook de decisão |
| `SMTP_HOST` / `SMTP_PORT` | `localhost` / `1025` | Servidor de e-mail das notificações |

---

## Governança e documentação

- **Constituição** (regras não-negociáveis): [`.specify/memory/constitution.md`](.specify/memory/constitution.md)
- **Contexto técnico** (módulos, máquina de estados, padrões, schema): [`docs/contexto-tecnico.md`](docs/contexto-tecnico.md)
- **Mapa de conformidade Fase 2**: [`docs/conformidade-fase2.md`](docs/conformidade-fase2.md)
- **Checklist de validação funcional**: [`docs/checklist-validacao-funcional-fase2.md`](docs/checklist-validacao-funcional-fase2.md)
- READMEs por domínio: `src/main/java/br/com/oficina/<dominio>/README.md`
