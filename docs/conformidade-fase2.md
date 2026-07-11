# Mapa de Conformidade — Tech Challenge Fase 2 (14SOAT)

**Data**: 2026-06-27 · **Fonte normativa**: [`docs/14SOAT - Fase 2 - Tech challenge.pdf`](14SOAT%20-%20Fase%202%20-%20Tech%20challenge.pdf) · **Spec**: [`specs/009-conformidade-fase2`](../specs/009-conformidade-fase2/spec.md)

Auditoria de conformidade da aplicação frente aos requisitos obrigatórios e entregáveis da Fase 2.

**Legenda**: ✅ Conforme · 🟡 Parcial · ❌ Ausente

**Resumo**: ✅ 13 · 🟡 1 · ❌ 4 · (fora de escopo) 1 — total de 19 itens auditados.

> A construção dos artefatos de infraestrutura ausentes (Kubernetes, Terraform, CI/CD) é tratada
> como esforço separado em features de acompanhamento (ver spec 009, seção "Out of Scope").
> Os alvos já estão decididos: **cluster Kubernetes local (kind/minikube) + banco em container via
> Terraform**, **pipeline em GitHub Actions**, com meta de **deploy real demonstrável**.

---

## Evolução da aplicação

| ID | Requisito | Estado | Evidência | Lacuna / Acompanhamento | FR(s) |
|---|---|---|---|---|---|
| APP-OS-ABERTURA | Abertura de OS recebendo cliente, veículo, serviços e peças, retornando identificação única | ✅ | `POST /ordens-servico` em `OrdemDeServicoController:112`; retorna `numeroOrdemServico` e `situacao="Recebida"` | — | FR-001 |
| APP-OS-STATUS | Consulta de status com as 6 situações (Recebida, Diagnóstico, Aguardando Aprovação, Execução, Finalizada, Entregue) | ✅ | `GET /ordens-servico/{num}` e `GET /ordens-servico/{num}/acompanhamento`; enum `SituacaoOrdemDeServico` mapeia os 6 rótulos | — | FR-002 |
| APP-ORC-WEBHOOK | Aprovação/recusa de orçamento por notificação externa | ✅ | `POST /integracoes/orcamentos/{num}/decisao` (`WebhookDecisaoOrcamentoController`); `X-Webhook-Token`; idempotente; decisão divergente → 409 | — | FR-003, FR-004, FR-005 |
| APP-OS-LISTAGEM | Listagem ordenada (Execução>Aguardando Aprovação>Diagnóstico>Recebida; mais antigas primeiro) com exclusão lógica de finalizadas/entregues | ✅ | `SpringDataOrdemDeServicoRepository.buscarAtivasPriorizadasPorFiltros` (query no banco): `status not in (OS_FINALIZADA, ENTREGUE)` + `order by` por prioridade e `iniciadaEm asc` | — | FR-006, FR-007 |
| APP-OS-NOTIFICACAO | Atualização de status via ferramenta (e-mail) | ✅ | Módulo `notificacao` com outbox + reprocessamento agendado (`EnviarNotificacaoStatusOSService`, `ReprocessamentoNotificacoesScheduler`); falha de SMTP não causa rollback | FR-008 |
| APP-REFAC-CLEANCODE | Clean Code (nomes claros, simplicidade, coesão) | ✅ | Arquitetura por módulo, factory methods de domínio, DTOs `record`; aderente à constituição (Princípios II, VII) | — | FR-020 |
| APP-ARQ-HEXAGONAL | Clean/Hexagonal Architecture (separação de camadas) | 🟡 | `pecainsumo` = referência correta (domínio puro + `*JpaEntity`); estrutura `domain`/`application`/`infrastructure` em todos os módulos | Dívida conhecida: `cliente`, `veiculo`, `ordemservico`, `orcamento` têm anotações JPA no aggregate root (constituição, Aderência por módulo). Acompanhamento: refatoração incremental, não agravar | FR-020 |
| APP-TESTES | Testes automatizados cobrindo fluxos críticos | ✅ | `./mvnw verify` BUILD SUCCESS: 353 testes, 0 falhas; JaCoCo "All coverage checks have been met"; geral 91% linhas; módulos de negócio (cliente, ordemservico, orcamento, veiculo, pecainsumo, notificacao) ≥ 80% | — | FR-021 |

---

## Infraestrutura

| ID | Requisito | Estado | Evidência | Lacuna / Acompanhamento | FR(s) |
|---|---|---|---|---|---|
| INFRA-DOCKER | Aplicação containerizada (Dockerfile + docker-compose para dev local) | ✅ | `Dockerfile` (build multiestágio Maven→JRE) e `docker-compose.yml` (serviços `db` e `api`) | — | FR-010 |
| INFRA-K8S | Manifestos Kubernetes: Deployments, Services, ConfigMaps/Secrets, HPA | ❌ | — | Diretório `/k8s` ausente. Acompanhamento: feature futura (alvo: cluster local kind/minikube; Secrets para `ORCAMENTO_WEBHOOK_SECRET`/`JWT_SECRET`; HPA por CPU/memória) | FR-013 |
| INFRA-IAC | Terraform provisionando cluster + banco, documentado | ❌ | — | Diretório `/infra` ausente. Acompanhamento: feature futura (alvo: cluster local + banco; documentar recursos e como aplicar) | FR-013 |
| INFRA-CICD | Pipeline CI/CD (build, testes, imagem, deploy app/banco/manifestos) | ❌ | — | `.github/workflows` ausente. Acompanhamento: feature futura (alvo: GitHub Actions; meta de deploy real demonstrável) | FR-013 |

---

## Documentação / Entregáveis

| ID | Requisito | Estado | Evidência | Lacuna / Acompanhamento | FR(s) |
|---|---|---|---|---|---|
| DOC-README-SOLUCAO | README com descrição da solução e objetivos da Fase 2 | ✅ | `README.md` reescrito (spec 009) | — | FR-014 |
| DOC-README-ARQUITETURA | README com desenho de arquitetura (componentes, infra, fluxo de deploy) | ✅ | `README.md` seção "Arquitetura"; infra/deploy marcados como 🟡 planejado | — | FR-015 |
| DOC-README-INSTRUCOES | Instruções de execução local / Kubernetes / Terraform | 🟡→✅ | `README.md` seção "Como executar": execução local funcional; K8s/Terraform documentados como roadmap | K8s/Terraform são instruções de roadmap até as features de infra | FR-016, FR-017 |
| DOC-README-LINKS | Link da collection das APIs + seção do vídeo | ✅ | `README.md`: link para `docs/collections/oficina-api.insomnia.json` e Swagger; seção reservada para o vídeo (placeholder até publicação) | Link do vídeo a preencher quando publicado (produção fora de escopo) | FR-018 |
| DOC-ENTREGA-PORTAL | PDF no portal do aluno + repo compartilhado com `soat-architecture` | (fora de escopo) | — | Responsabilidade do time na submissão; fora do escopo de código desta feature | — |

---

## Observações de curadoria (débitos da constituição reavaliados)

Durante a auditoria, dois "Débitos Técnicos Conhecidos" da constituição foram constatados como **já tratados** no código atual:

- **Logging em `ordemservico`**: os 20 services do módulo possuem logging estruturado SLF4J (ex.: `ConsultarOrdensDeServicoService:37`). Débito **resolvido**.
- **Filtro de listagem de OS em memória**: a listagem é feita por query no banco (`buscarAtivasPriorizadasPorFiltros`), não mais via `findAll().stream()`. Débito **resolvido**.

Correção aplicada nesta feature (achado bloqueante encontrado na verificação — T008):

- **`cliente.buscarPorDocumento` insensível à formatação**: o teste de integração `JpaClienteRepositoryIntegrationTest.deveBuscarClientePorNomeEDocumentoIgnorandoFormatacao` falhava porque a busca por documento fazia match exato. Corrigido com query normalizada no banco (`findByDocumentoNormalizado`, via `replace()` — compatível com H2 e PostgreSQL), preservando o armazenamento do documento como informado e a correção do CRI-002 (sem busca em memória).
