# ADR 0002 — Snapshot de dados em ordens de serviço e orçamentos

- **Status:** Aceito
- **Data:** 2026-09-12
- **Contexto:** Tech Challenge SOAT 13 — modelagem herdada da Fase 2, formalizada agora

## Contexto

As tabelas `ordens_de_servico` e `orcamentos` guardam, além das chaves
estrangeiras para cliente, veículo e funcionário, **cópias dos dados dessas
entidades** no momento da criação:

```
ordens_de_servico
  cliente_id        FK → clientes
  cliente_nome      cópia
  cliente_documento cópia
  cliente_tipo      cópia
  funcionario_id    FK → funcionarios
  funcionario_nome  cópia
  funcionario_cpf   cópia
  veiculo_id        FK → veiculos
  veiculo_placa, veiculo_marca, veiculo_modelo,
  veiculo_fabricante, veiculo_ano, veiculo_potencia,
  veiculo_cambio, veiculo_tipo                    cópias
```

Vista sem contexto, essa duplicação parece erro de normalização — o tipo de coisa
que uma revisão automática marca como violação da 3FN. A decisão precisa estar
escrita, porque é deliberada e tem custo.

## Decisão

**Manter o snapshot.** Ordem de serviço e orçamento são **documentos
históricos**, não projeções do estado atual das entidades que referenciam.

O raciocínio: uma OS emitida em março registra que o veículo de placa `ABC1D23`,
um Corolla 2020, pertencia ao cliente João e foi atendido pelo funcionário Maria.
Se o cliente troca de nome, se a placa é transferida ou se o funcionário sai da
empresa, **a OS de março não muda** — ela documenta o que era verdade quando foi
emitida.

Sem o snapshot, uma consulta à OS antiga faria `JOIN` com o estado **atual** e
exibiria dados que não correspondem ao documento original. Pior: uma exclusão de
cliente tornaria a OS ilegível ou impossível de carregar.

As chaves estrangeiras continuam existindo e servem a outro propósito —
navegação e integridade ("todas as OS deste cliente"). As duas coisas coexistem
porque respondem a perguntas diferentes: a FK responde *quem é hoje*, o snapshot
responde *quem era então*.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| **Normalizar, só FK** | Modelo mais limpo e historicamente errado: a OS passaria a refletir o estado atual das entidades, não o do momento da emissão. Para documento fiscal ou de serviço, isso é defeito, não elegância. |
| **Versionar cliente, veículo e funcionário** (SCD tipo 2) | Historicamente correto e a solução canônica. Triplica a complexidade do modelo: toda consulta passa a precisar de filtro por vigência, e três entidades ganham ciclo de vida próprio. Desproporcional para um domínio onde só dois agregados precisam de histórico. |
| **Snapshot em JSON numa coluna** | Concentraria a duplicação num campo só. Perde tipagem, indexação e legibilidade em consulta — e a OS é justamente o que mais se consulta. |
| **Guardar só o essencial** (nome e documento, sem os dados do veículo) | Reduziria a duplicação, mas a OS impressa mostra marca, modelo e ano do veículo. Buscar isso do estado atual reintroduz o problema, só que parcialmente — o que é pior que assumi-lo inteiro. |

## Consequências

**Positivas**

- A OS e o orçamento são autocontidos: leem-se sem depender do estado atual de nada.
- Exclusão ou alteração de cliente, veículo ou funcionário não corrompe documentos emitidos.
- Consultas de listagem não precisam de `JOIN` para exibir o essencial, o que também ajuda no desempenho.

**Negativas e mitigações**

- **Dado duplicado ocupa espaço.** Irrelevante na escala deste domínio: são colunas de texto curto em duas tabelas.
- **Divergência é esperada, não é bug.** Cliente renomeado aparece com o nome antigo em OS antigas. É o comportamento correto, mas contraintuitivo para quem lê o schema sem este documento — que é a razão de ele existir.
- **Correção retroativa é impossível pelo modelo.** Se o nome foi gravado errado na emissão, só um `UPDATE` explícito conserta. Aceito: é a mesma limitação de qualquer documento emitido.
- **Ferramentas de análise vão apontar violação de normalização.** Esperado. A 3FN otimiza para consistência do estado atual; documento histórico otimiza para imutabilidade. São objetivos diferentes, e aqui o segundo vence.

## Relacionado

- [`../arquitetura/modelo-de-dados.md`](../arquitetura/modelo-de-dados.md) — diagrama ER completo e demais ajustes do modelo
- [`../arquitetura/diagramas-sequencia.md`](../arquitetura/diagramas-sequencia.md) — o snapshot sendo gravado na abertura da OS
