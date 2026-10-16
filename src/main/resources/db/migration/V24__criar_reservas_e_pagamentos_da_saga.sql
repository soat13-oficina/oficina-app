-- Fase 4, Passo 1 (3a entrega): reserva de pecas e pagamento viram passos da Saga (ADR 0004).
--
-- reservas_pecas: o que cada OS tem reservado de cada peca. Ate aqui a reserva era so um contador
-- em pecas_insumos.quantidade_reservada, sem dono: nao dava para liberar "a reserva desta OS" nem
-- responder de novo a um ReservarPecas reenviado sem reservar duas vezes. Na Execution Service esta
-- tabela vira oficina-<env>-reservas no DynamoDB (PK ordemServicoId, SK pecaId).
CREATE TABLE IF NOT EXISTS reservas_pecas (
    ordem_de_servico_id  UUID         NOT NULL,              -- sem FK: a OS mora em outro servico
    peca_insumo_id       VARCHAR(255) NOT NULL REFERENCES pecas_insumos (id),
    quantidade           INT          NOT NULL CHECK (quantidade > 0),
    status               VARCHAR(20)  NOT NULL,              -- RESERVADA, CONSUMIDA, LIBERADA
    criada_em            TIMESTAMP    NOT NULL,
    atualizada_em        TIMESTAMP    NOT NULL,
    PRIMARY KEY (ordem_de_servico_id, peca_insumo_id)
);

-- Regularizacao: OSs aprovadas antes da Saga tiveram as pecas reservadas so no contador. Sem uma
-- linha aqui, a conclusao do servico (que agora consome a reserva da OS) nao baixaria o estoque delas.
INSERT INTO reservas_pecas (ordem_de_servico_id, peca_insumo_id, quantidade, status, criada_em, atualizada_em)
SELECT o.ordem_de_servico_id, p.peca_insumo_id, SUM(p.quantidade), 'RESERVADA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM orcamentos o
  JOIN orcamento_pecas_previstas p ON p.orcamento_id = o.id
  JOIN ordens_de_servico os ON os.id = o.ordem_de_servico_id
 WHERE o.status = 'APROVADO'
   AND os.status IN ('AGUARDANDO_EXECUCAO', 'SERVICO_EM_ANDAMENTO')
   AND p.peca_insumo_id IS NOT NULL
   AND EXISTS (SELECT 1 FROM pecas_insumos pi WHERE pi.id = p.peca_insumo_id)
 GROUP BY o.ordem_de_servico_id, p.peca_insumo_id
ON CONFLICT DO NOTHING;

-- pagamentos: cobranca de um orcamento aprovado (ADR 0007). Nesta entrega o gateway e simulado; o
-- Mercado Pago entra no Passo 3, junto com a extracao do Billing - por isso ja existem as colunas mp_*.
CREATE TABLE IF NOT EXISTS pagamentos (
    id                   UUID          PRIMARY KEY,
    orcamento_id         UUID          NOT NULL REFERENCES orcamentos (id),
    ordem_de_servico_id  UUID          NOT NULL,             -- sem FK: dado de outro servico
    saga_id              UUID          NOT NULL,
    valor                NUMERIC(12,2) NOT NULL,
    status               VARCHAR(30)   NOT NULL,             -- PENDENTE, APROVADO, RECUSADO, EXPIRADO, ESTORNADO, CANCELADO
    motivo               VARCHAR(500),
    mp_preference_id     VARCHAR(80),
    mp_payment_id        VARCHAR(40)   UNIQUE,
    mp_refund_id         VARCHAR(40),
    link_pagamento       VARCHAR(1000),
    expira_em            TIMESTAMP     NOT NULL,
    idempotency_key      UUID          NOT NULL UNIQUE,
    versao               BIGINT        NOT NULL DEFAULT 0,
    criado_em            TIMESTAMP     NOT NULL,
    atualizado_em        TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pagamentos_orcamento ON pagamentos (orcamento_id);

-- O expirador do Billing so le pagamentos pendentes.
CREATE INDEX IF NOT EXISTS idx_pagamentos_pendentes ON pagamentos (expira_em) WHERE status = 'PENDENTE';

-- A Saga guarda o link de pagamento recebido em PagamentoSolicitado: aparece em GET /sagas/{id}.
ALTER TABLE saga_instancia ADD COLUMN IF NOT EXISTS link_pagamento VARCHAR(1000);
