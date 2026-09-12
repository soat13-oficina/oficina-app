# ADR 0001 — Estratégia de escala: HPA e node group

- **Status:** Aceito, com limitação conhecida
- **Data:** 2026-09-12
- **Contexto:** Tech Challenge SOAT 13 — Fase 3

## Contexto

O enunciado exige "cluster Kubernetes **com escalabilidade**". Escalabilidade em
Kubernetes tem dois níveis independentes, e atender só um deixa o outro como
gargalo:

- **Pods** — quantas réplicas da aplicação atendem a carga
- **Nós** — quanta máquina existe para essas réplicas ocuparem

Escalar pods num cluster de tamanho fixo esbarra em `Pending` assim que os nós
enchem. Escalar nós sem escalar pods paga máquina ociosa.

## Decisão

**Os dois níveis, com responsabilidades separadas por repositório.**

### Nível de pod — HorizontalPodAutoscaler

Definido em `k8s/base/hpa.yaml`, com a faixa ajustada por ambiente:

| | `hml` | `prd` |
|---|---|---|
| `minReplicas` | 1 | 2 |
| `maxReplicas` | 2 | 4 |

Produção parte de 2 réplicas por disponibilidade — uma réplica única cai junto
com o nó que a hospeda. Homologação parte de 1 porque indisponibilidade ali não
tem custo.

O comportamento é assimétrico de propósito: `scaleUp` com
`stabilizationWindowSeconds: 0` (sobe imediatamente ao detectar pressão) e
`scaleDown` com 60 s (desce com calma, evitando oscilação). Subir tarde custa
requisição perdida; descer tarde custa centavos.

Depende do add-on **`metrics-server`**, instalado como parte do cluster em
`oficina-infra-k8s`. Sem ele o HPA não tem de onde ler utilização e fica inerte —
por isso o add-on é responsabilidade da camada de plataforma, não da aplicação.

### Nível de nó — managed node group

Configurado em `oficina-infra-k8s`: de 2 a 4 instâncias `t3.medium`. O mínimo de
2 garante que a perda de um nó não derrube o cluster.

### Contenção entre ambientes — ResourceQuota

Como a plataforma é compartilhada (ver
[ADR 0002 de `oficina-infra-k8s`](https://github.com/soat13-oficina/oficina-infra-k8s/blob/master/docs/adr/0002-plataforma-compartilhada.md)),
cada namespace tem cota de CPU, memória e número de pods. É ela que impede um
vazamento em homologação de consumir os nós e derrubar produção junto — e é o
que torna a plataforma compartilhada defensável em vez de arriscada.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| **Apenas HPA, cluster fixo** | Mais simples, mas o HPA bate no teto de capacidade e os pods ficam `Pending`. Escalabilidade só no papel. |
| **Apenas node group elástico** | Nós entram, mas as réplicas continuam as mesmas — paga-se máquina sem aumentar vazão. |
| **KEDA / métricas customizadas** (ex.: profundidade da fila de notificações) | Escalaria pelo sinal mais próximo do negócio. Exige um componente a mais no cluster e um adapter de métricas, desproporcional para um domínio sem fila externa. |
| **Cluster Autoscaler ou Karpenter** | Substituiriam o dimensionamento fixo do node group por provisionamento reativo. Ganho real em produção, complexidade desnecessária num cluster de 2 a 4 nós. |

## Consequências

**Positivas**

- Os dois níveis se complementam: o HPA reage em segundos, o node group em minutos.
- A faixa por ambiente é um patch de overlay — mudar a escala de homologação não toca produção.
- A cota por namespace transforma "ambientes compartilhados" de risco em decisão com contenção.

**Negativa — limitação conhecida e verificada**

> **Escalar JVM por utilização de memória não funciona, e a configuração atual
> ainda usa essa métrica.**

Observado no ambiente real, com a aplicação ociosa em homologação:

```
horizontalpodautoscaler/oficina-api   cpu: 19%/70%, memory: 164%/80%   1   2   REPLICAS 2
```

A JVM aloca heap até o limite de `MaxRAMPercentage` e **não devolve ao sistema
operacional**. O uso de memória fica permanentemente alto *por projeto*, não por
pressão. O HPA lê isso como demanda constante, escala até o teto e nunca desce —
com CPU em 19%, ou seja, sem carga nenhuma.

O efeito prático é duplo: o autoscaling deixa de ser autoscaling (fica cravado no
máximo) e a demonstração de escalabilidade perde o sentido, porque não há para
onde crescer quando a carga de fato chegar.

**Correção recomendada:** remover a métrica de memória do HPA, deixando só CPU —
que em 19% reflete carga real e responde a estímulo. E elevar `requests.memory`
para ~512Mi, já que os pods medem ~447Mi em repouso e homologação tem limite de
512Mi, ou seja, ~65Mi de folga até o `OOMKill`.

Não aplicada nesta fase por decisão de cronograma, com o ambiente já validado e
gravado. Fica registrada aqui como dívida conhecida, com diagnóstico e correção
prontos — e não como descoberta pendente.
