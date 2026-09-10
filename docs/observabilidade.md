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

| Métrica (código) | Em `/actuator/prometheus` | No Datadog | Tags | Responde |
|---|---|---|---|---|
| `os.criadas` | `os_criadas_total` | `oficina.os.criadas.count` | `ambiente` | Volume diário de OS |
| `os.transicoes` | `os_transicoes_total` | `oficina.os.transicoes.count` | `ambiente`, `situacao_anterior`, `situacao` | Fluxo entre etapas |
| `os.tempo_na_situacao` | `os_tempo_na_situacao_seconds_{count,sum}` | `oficina.os.tempo_na_situacao.{count,sum}` | `ambiente`, `situacao` | Tempo médio por status |
| `os.tempo_na_situacao` | `os_tempo_na_situacao_seconds_max` | `oficina.os.tempo_na_situacao.max` | `ambiente`, `situacao` | Pior caso na janela |
| `notificacoes.falhas` | `notificacoes_falhas_total` | `oficina.notificacoes.falhas.count` | `ambiente` | Alerta de entrega perdida |

**Por que o nome muda três vezes no caminho.** Nenhuma das três é opcional:

1. **Micrometer** troca `.` por `_` e acrescenta sufixo de tipo — `os.criadas` vira
   `os_criadas_total`, e o *timer* `os.tempo_na_situacao` vira a summary
   `os_tempo_na_situacao_seconds` (com `_count`/`_sum`) mais o gauge `..._seconds_max`.
2. **O check OpenMetrics v2** exige o counter declarado **sem** o `_total` e devolve os
   valores com sufixo `.count` (counter e `_count` de summary) e `.sum`.
3. **`namespace: oficina`** é obrigatório no check e é prependado a tudo — por isso o
   código não carrega o prefixo, que produziria `oficina.oficina...`.

O mapeamento explícito na anotação `ad.datadoghq.com/oficina-api.checks`
(`k8s/base/deployment.yaml`) converte de volta para nomes com ponto e torna o resultado
determinístico. Sem ele a métrica chegaria como `oficina.os_criadas.count` e a entrada
`os_tempo_na_situacao` sequer casaria com a família real, que termina em `_seconds`.

**Tempo médio** é `sum / count` (o resultado sai em **segundos**), não uma métrica
pronta — é assim que o dashboard e a tabela por situação calculam.

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

Linha real emitida com `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` (campos irrelevantes
omitidos) — é o formato que o Agent envia e que os widgets e o monitor de log consomem:

```json
{
  "@timestamp": "2026-09-09T12:56:00.970861508Z",
  "log": { "level": "INFO", "logger": "br.com.oficina.ordemservico.infrastructure.web.OrdemDeServicoController" },
  "service": { "name": "oficina-api", "version": "0.0.1-SNAPSHOT" },
  "message": "Recebida requisicao de consulta de status. numeroOrdemServico=OS-5494D888",
  "correlation_id": "teste-correlacao-123",
  "numero_ordem_servico": "OS-5494D888"
}
```

Os campos do MDC saem no **nível raiz**, e por isso viram atributos pesquisáveis
`@correlation_id` e `@numero_ordem_servico` no Datadog. O nível vira `@log.level` e o
logger, `@log.logger` — é neste último que o monitor de integrações filtra
`br.com.oficina.notificacao.*` e `br.com.oficina.orcamento.*`.

**`service.name` precisa ser `oficina-api`.** É o mesmo nome do `DD_SERVICE`, do label
`tags.datadoghq.com/service` do pod e do serviço no APM. Ele vem de
`spring.application.name`: divergir ali faz o log chegar sob outro serviço e quebra
justamente a correlação log ↔ trace.

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

**Resultado vazio numa aplicação recém-subida é o esperado, não um defeito.** O
Micrometer registra o *meter* na primeira vez que ele é incrementado, então nenhuma
série de `os_*` existe antes da primeira OS criada. Crie uma OS e faça uma transição
antes de concluir que a instrumentação não está funcionando.

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

## Dashboard e alertas

Vivem como código no `oficina-infra-k8s` (`datadog-dashboard.tf`,
`datadog-monitores.tf`, `datadog-uptime.tf`), não na UI da Datadog. Editar pela
interface funciona até o próximo `terraform apply`, que reverte.

O dashboard **Oficina — Ordens de Serviço e Plataforma**
(`datadog/dashboard-oficina.json`) tem quatro grupos: negócio (volume diário, tempo médio
por situação, transições), APIs (latência p50/p95/p99, taxa de 5xx, endpoints mais
lentos), Kubernetes (CPU e memória contra o *limite*, réplicas prontas, reinícios) e
falhas (SLO de disponibilidade, notificações perdidas, stream de logs de erro).

Oito monitors, cobrindo os quatro sinais que a tarefa pede alerta:

| # | Monitor | Sinal |
|---|---|---|
| 1 | `oficina.notificacoes.falhas.count` > 0 em 15 min | Falha no processamento de OS |
| 2 | Taxa de 5xx > 5% (warning 2%) | Erro nas APIs |
| 3 | p95 acima do alvo (`datadog_latencia_p95_segundos`) | Latência das APIs |
| 4 | Memória do pod > 90% do limite | CPU/memória no Kubernetes |
| 5 | CPU do pod > 90% do limite | CPU/memória no Kubernetes |
| 6 | Réplicas prontas abaixo do desejado (+ *no data*) | Healthcheck |
| 7 | Container em `CrashLoopBackOff` | Healthcheck |
| 8 | Erros de log em `notificacao.*` / `orcamento.*` | Integrações (SES, webhook) |

Mais o teste sintético HTTP de duas regiões, que alimenta o SLO de disponibilidade.

**Por que o monitor 1 alerta em `> 0`**: `notificacoes.falhas` só incrementa depois das 5
tentativas (`notificacao.reprocessamento.max-tentativas`). Falha transiente ainda será
reprocessada e "cliente sem e-mail" é cadastro incompleto — nenhuma das duas chega lá.
Chegar significa cliente sem aviso, sem nova tentativa.

**Não alertar em `RegraDeNegocioException`**: é 4xx e uso normal da API. Alertar nela
acordaria alguém porque um usuário digitou uma placa errada.

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
