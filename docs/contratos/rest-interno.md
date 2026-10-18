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
| Dono | Execution Service (`ConsultaInternaPecasController`) |
| Chamadores | Billing Service, ao processar `GerarOrcamento` (preço); OS Service, ao abrir a ordem com peças previstas (existência) |

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

## `GET /internal/clientes/por-cpf/{cpf}`

Cliente pelo CPF, para a Lambda de autenticação emitir o token (substitui a leitura direta do banco do OS).

| | |
|---|---|
| Dono | OS Service (`ConsultaInternaClientesController`) |
| Chamador | `oficina-lambda-auth` (função `token`), com timeout de 3 s |
| Caminho | **Não** passa pelo API Gateway. A Lambda chega ao OS pelo ALB, em regra que só encaminha `/internal/clientes/*` com o header `X-Service-Token` (a aplicação confere o valor) |

- `{cpf}` com ou sem máscara; só os dígitos são comparados (o cadastro guarda o documento como foi digitado). Diferente de 11 dígitos → `400`.

Resposta `200`:

```json
{ "id": "3f1c9a2e-0000-4000-8000-000000000001", "nome": "Maria da Silva", "ativo": true }
```

- Cliente **inativo** também volta (`ativo: false`): quem recusa o token (`403 CLIENTE_INATIVO`) é a Lambda.
- Sem cadastro → `404`. Token de serviço ausente ou errado → `401`.

