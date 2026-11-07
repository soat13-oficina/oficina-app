-- Historico de status da ordem de servico (Fase 4: "consulta de status e HISTORICO").
--
-- Ate aqui so existia o status atual e situacao_alterada_em: nao havia como responder
-- "por onde esta OS passou e quando". Com a Saga (ADR 0004) o status passa a mudar tambem
-- por mensagens e timeouts, e a origem de cada mudanca precisa ficar registrada.
--
-- Uma linha por mudanca de STATUS (nao de situacao): DIAGNOSTICO_EM_ANDAMENTO ->
-- DIAGNOSTICO_CONCLUIDO aparece no historico mesmo sendo a mesma situacao "Diagnostico".
--
-- origem: API (requisicao HTTP), SAGA (evento processado pelo orquestrador), TIMEOUT
-- (prazo vencido) ou MIGRACAO (linha criada por esta migration para OSs ja existentes).
CREATE TABLE IF NOT EXISTS ordem_servico_historico (
    id                  BIGSERIAL    PRIMARY KEY,
    ordem_de_servico_id UUID         NOT NULL REFERENCES ordens_de_servico (id) ON DELETE CASCADE,
    status_anterior     VARCHAR(40),
    status_novo         VARCHAR(40)  NOT NULL,
    origem              VARCHAR(20)  NOT NULL,
    saga_id             UUID,
    ocorrido_em         TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ordem_servico_historico_ordem
    ON ordem_servico_historico (ordem_de_servico_id, ocorrido_em);

-- OSs anteriores ao historico ganham uma linha com o status atual, para que a consulta nunca
-- volte vazia. O instante e o melhor conhecido (mesma regra do backfill da V19); sem nenhum,
-- vale o momento da migration.
INSERT INTO ordem_servico_historico (ordem_de_servico_id, status_anterior, status_novo, origem, ocorrido_em)
SELECT os.id, NULL, os.status, 'MIGRACAO',
       COALESCE(os.situacao_alterada_em, os.entregue_em, os.finalizada_em, os.iniciada_em, CURRENT_TIMESTAMP)
  FROM ordens_de_servico os
 WHERE NOT EXISTS (SELECT 1 FROM ordem_servico_historico h WHERE h.ordem_de_servico_id = os.id);
