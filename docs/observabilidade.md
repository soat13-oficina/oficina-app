# Observabilidade

Como a aplicação é instrumentada, o que ela emite e como conferir cada coisa sem
precisar de conta na Datadog. A decisão e as alternativas descartadas estão no
**ADR 0003 — Observabilidade com Datadog**, em
`oficina-infra-k8s/docs/adr/0003-observabilidade-datadog.md`.

## Visão geral

| Sinal | Origem | Onde aparece |
|---|---|---|
| Latência e erros das APIs | `dd-java-agent` (autoinstrumentação) | APM — Datadog |
| CPU/memória de pods e nodes | Datadog Agent + kube-state-metrics | Infra — Datadog |
| Healthcheck / uptime | `/actuator/health/{liveness,readiness}` | probes do k8s + monitor |
| Métricas de negócio | `@EventListener` sobre eventos de domínio | `/actuator/prometheus` → Datadog |
| Logs JSON correlacionados | Structured logging do Spring Boot 4 (ECS) | Logs — Datadog |

Nenhum caso de uso conhece Micrometer ou Datadog. Toda a instrumentação de
negócio vive em dois listeners:

- `ordemservico/infrastructure/observabilidade/MetricasOrdemServicoListener`
- `notificacao/infrastructure/observabilidade/MetricasNotificacaoListener`

## Métricas de negócio

| Métrica (código) | No Datadog | Tipo | Tags | Responde |
|---|---|---|---|---|
| `os.criadas` | `oficina.os.criadas` | counter | `ambiente` | Volume diário de OS |
| `os.transicoes` | `oficina.os.transicoes` | counter | `situacao_anterior`, `situacao` | Fluxo entre etapas |
| `os.tempo_na_situacao` | `oficina.os.tempo_na_situacao` | timer | `situacao` | Tempo médio por status |
| `notificacoes.falhas` | `oficina.notificacoes.falhas` | counter | `ambiente` | Alerta de entrega perdida |

**Por que os nomes diferem.** O check OpenMetrics do Datadog Agent exige um
`namespace` e o prepende a tudo que raspa. Com `namespace: oficina` (definido na
anotação `ad.datadoghq.com/oficina-api.checks` em `k8s/base/deployment.yaml`), a
métrica `os.criadas` chega como `oficina.os.criadas`. Manter o prefixo no código
produziria `oficina.oficina.os.criadas`.

**Cardinalidade.** As tags saem sempre de `SituacaoOrdemDeServico` — seis valores
fechados. Número de OS e id de cliente **não** viram tag; eles vão para o MDC dos
logs, onde a busca por uma ordem específica é barata.

### Onde `os.tempo_na_situacao` nasce

A coluna `situacao_alterada_em` (migration **V19**) marca quando a ordem entrou na
situação em que está. O evento `StatusOrdemDeServicoAlterado` carrega
`anteriorDesde` (o valor lido *antes* da transição) e `ocorridoEm`; a diferença é
o tempo que a ordem passou na etapa que acabou de terminar.

O relógio segue a **situação de negócio**, não o status interno:
`DIAGNOSTICO_EM_ANDAMENTO → DIAGNOSTICO_CONCLUIDO` são dois status dentro de
"Diagnóstico" e **não** reiniciam a contagem.

Ordens anteriores à V19 sem timestamp para o backfill chegam com `anteriorDesde`
nulo: o listener conta a transição e **pula** o timer, em vez de registrar zero e
puxar a média da etapa para baixo.

## Logs

Em nuvem, `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` faz o Spring Boot 4 emitir JSON
no formato Elastic Common Schema. Localmente a variável não é definida e o console
segue no formato humano.

Campos de correlação no MDC (que o ECS serializa no nível raiz do JSON):

| Campo | Origem | Cobre |
|---|---|---|
| `correlation_id` | `CorrelationIdFilter` | Toda requisição HTTP; aceita `X-Correlation-Id` ou `X-Request-Id` de entrada e devolve em `X-Correlation-Id` |
| `numero_ordem_servico` | `OrdemDeServicoMdcInterceptor` | Toda rota com `{numeroOrdemServico}` no path |
| `dd.trace_id` / `dd.span_id` | `dd-java-agent` com `DD_LOGS_INJECTION=true` | Correlação log ↔ trace |

O `TaskDecorator` em `AsyncConfig` propaga o MDC para as threads do pool — sem
ele o envio de notificação (`@Async`) logaria sem correlação, justamente no ponto
onde mais se precisa dela para investigar entrega falha.

Buscar o rastro completo de uma ordem:

```
service:oficina-api numero_ordem_servico:OS-1A2B3C4D
```

## Portas

| Porta | O que serve | Exposta no `Service`? |
|---|---|---|
| 8080 | API | **Sim** — `type: LoadBalancer`, NLB público |
| 8081 | Actuator (`health`, `prometheus`) em hml/prd | **Não**, de propósito |

`service.yaml` provisiona um NLB público: tudo que responde na 8080 está na
internet. Por isso `MANAGEMENT_SERVER_PORT=8081` no `deployment.yaml` base. Probes
(kubelet) e o Datadog Agent falam com o IP do pod diretamente e não dependem do
`Service` para alcançar a 8081.

Localmente nada muda — a variável não é definida e tudo continua na 8080.

## Como conferir localmente

Sem conta na Datadog, sem cluster:

```bash
docker compose up -d
curl -s localhost:8080/actuator/health/readiness

# Métricas de negócio (nomes sem o prefixo "oficina.", ver acima)
curl -s localhost:8080/actuator/prometheus | grep -E '^os_|^notificacoes_'
```

Exercitando o fluxo e vendo o contador subir:

```bash
# antes
curl -s localhost:8080/actuator/prometheus | grep '^os_criadas_total'
# ... crie uma OS pela API ...
# depois: o valor subiu 1
```

O correlation id volta no cabeçalho da resposta:

```bash
curl -si localhost:8080/actuator/health/liveness | grep -i x-correlation-id
```

## Alertas

Três sinais valem monitor. O resto é ruído:

1. **`oficina.notificacoes.falhas`** — notificação que esgotou as 5 tentativas
   (`notificacao.reprocessamento.max-tentativas`). É o único estado terminal de
   erro que significa perda real: falha transiente ainda será reprocessada e
   "cliente sem e-mail" é cadastro incompleto, não incidente.
2. **5xx no APM.**
3. **Erros de SES e do webhook de orçamento.**

**Não alertar em `RegraDeNegocioException`**: é 4xx e uso normal da API.

## Tracer

O `dd-java-agent` é baixado no build e fica **sempre** na imagem, mas só é
carregado quando os overlays de nuvem acrescentam `-javaagent` a `JAVA_OPTS` —
assim a mesma imagem roda no `docker-compose` local sem tentar falar com um agent
que não existe ali.

A versão é fixada (`ARG DD_TRACER_VERSION` no `Dockerfile`). O atalho
`dtdg.co/latest-java-tracer` resolveria para o último release e faria dois builds
do mesmo commit embarcarem tracers diferentes.

## Custo

O free tier da Datadog cobre métricas de infra (5 hosts), mas **não** cobre APM
nem logs. O caminho é o **trial de 14 dias, ativado perto da demo** — o
`helm_release` no `oficina-infra-k8s` é opt-in por `datadog_api_key` e fica
inerte até lá.
