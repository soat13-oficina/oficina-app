# Contratos REST internos

Chamadas síncronas entre serviços ([ADR 0003](../adr/0003-divisao-em-microsservicos-e-dono-dos-dados.md)).
Nunca passam pelo API Gateway: dentro do cluster, pelo DNS do Kubernetes
(`http://oficina-<servico>:8080`).

## Regras comuns

- Header **`X-Service-Token`** com o segredo `oficina/<env>/service-token`. Ausente ou diferente →
  `401`. Serviço sem o segredo configurado recusa todas as chamadas internas.
- Header `x-request-id` propagado (correlação de logs e traces).
- Chamador com timeout curto (2–3 s) e retry com backoff só em operação idempotente — as consultas
  abaixo são.
- Dinheiro como texto com duas casas (`"80.00"`), como nas mensagens.

## `POST /internal/pecas-insumos/consulta`

Descrição e preço atuais das peças, para o Billing montar o orçamento.

| | |
|---|---|
| Dono | Execution Service (no Passo 3, ainda o OS Service, módulo `pecainsumo`) |
| Chamador | Billing Service, ao processar `GerarOrcamento` |

Requisição:

```json
{ "pecaInsumoIds": ["5e8f1a2b-3c4d-4e5f-8a6b-7c8d9e0f1a2b", "8a9b0c1d-2e3f-4a5b-9c6d-7e8f9a0b1c2d"] }
```

- De 1 a 200 ids; repetidos são ignorados. Lista vazia ou maior → `400`.

Resposta `200`:

```json
{
  "pecas": [
    { "pecaInsumoId": "5e8f1a2b-3c4d-4e5f-8a6b-7c8d9e0f1a2b", "descricao": "Filtro de oleo", "preco": "80.00" }
  ],
  "inexistentes": ["8a9b0c1d-2e3f-4a5b-9c6d-7e8f9a0b1c2d"]
}
```

- `pecas` na ordem pedida; `inexistentes` = ids que não estão no cadastro. O Billing responde
  `OrcamentoFalhou` com `pecasInvalidas` = `inexistentes` quando a lista não está vazia.
