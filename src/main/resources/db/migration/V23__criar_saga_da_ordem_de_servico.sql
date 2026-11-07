-- Saga da ordem de servico (ADR 0004): estado por OS e a historia de cada Saga.
--
-- Desvio consciente do SQL do ADR: em vez de uma coluna "dados" jsonb, o que a Saga guarda vai em
-- colunas proprias (desfecho, motivo, compensacoes_pendentes). Sao poucos campos, todos consultados
-- pelo agendador e pelo GET /sagas/{ordemServicoId}, e colunas tipadas sao mais faceis de ler e testar.
CREATE TABLE IF NOT EXISTS saga_instancia (
    id                      UUID         PRIMARY KEY,
    ordem_de_servico_id     UUID         NOT NULL UNIQUE REFERENCES ordens_de_servico (id) ON DELETE CASCADE,
    status                  VARCHAR(40)  NOT NULL,
    passo_atual             VARCHAR(40)  NOT NULL,
    orcamento_id            UUID,
    pagamento_id            UUID,
    desfecho                VARCHAR(40),
    motivo                  VARCHAR(255),
    compensacoes_pendentes  VARCHAR(255),
    prazo_expira_em         TIMESTAMP,
    tentativas_passo        INT          NOT NULL DEFAULT 0,
    versao                  BIGINT       NOT NULL DEFAULT 0,   -- lock otimista (consumidor x agendador)
    criada_em               TIMESTAMP    NOT NULL,
    atualizada_em           TIMESTAMP    NOT NULL
);

-- O agendador so le Sagas com prazo; o indice parcial ignora as que nao esperam nada.
CREATE INDEX IF NOT EXISTS idx_saga_instancia_prazo
    ON saga_instancia (prazo_expira_em) WHERE prazo_expira_em IS NOT NULL;

CREATE TABLE IF NOT EXISTS saga_passos (
    id           BIGSERIAL     PRIMARY KEY,
    saga_id      UUID          NOT NULL REFERENCES saga_instancia (id) ON DELETE CASCADE,
    passo        VARCHAR(40)   NOT NULL,
    tipo         VARCHAR(20)   NOT NULL,   -- INICIO, ADOCAO, ACAO_REGISTRADA, COMANDO_ENVIADO, EVENTO_RECEBIDO, EVENTO_IGNORADO, TIMEOUT
    mensagem_id  UUID,
    detalhe      VARCHAR(2000),
    ocorrido_em  TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_saga_passos_saga ON saga_passos (saga_id, ocorrido_em);

-- O orcamento guarda a Saga que o originou: vai no envelope dos eventos que ele publica.
-- Sem FK de proposito: no Billing Service, saga_instancia estara em outro banco.
ALTER TABLE orcamentos ADD COLUMN IF NOT EXISTS saga_id UUID;

-- Regularizacao: ate aqui, aprovar pelo JWT (POST /orcamentos/{id}/aprovacao) aprovava o orcamento
-- mas deixava a OS em AGUARDANDO_APROVACAO, e o mecanico iniciava a execucao a partir dali. Na Fase 4
-- a aprovacao leva a OS para AGUARDANDO_EXECUCAO; as OSs nesse meio-termo vao para la tambem.
INSERT INTO ordem_servico_historico (ordem_de_servico_id, status_anterior, status_novo, origem, ocorrido_em)
SELECT os.id, os.status, 'AGUARDANDO_EXECUCAO', 'MIGRACAO', CURRENT_TIMESTAMP
  FROM ordens_de_servico os
 WHERE os.status = 'AGUARDANDO_APROVACAO'
   AND EXISTS (SELECT 1 FROM orcamentos o WHERE o.ordem_de_servico_id = os.id AND o.status = 'APROVADO');

UPDATE ordens_de_servico os
   SET status = 'AGUARDANDO_EXECUCAO'
 WHERE os.status = 'AGUARDANDO_APROVACAO'
   AND EXISTS (SELECT 1 FROM orcamentos o WHERE o.ordem_de_servico_id = os.id AND o.status = 'APROVADO');

-- Uma Saga para cada OS existente, no estado correspondente ao status atual (mesma regra de
-- SagaOrdemServico.adotar). Sagas adotadas nao tem prazo: so as novas contam o tempo de decisao.
INSERT INTO saga_instancia (id, ordem_de_servico_id, status, passo_atual, criada_em, atualizada_em)
SELECT gen_random_uuid(),
       os.id,
       CASE
           WHEN os.status IN ('OS_ABERTA', 'DIAGNOSTICO_EM_ANDAMENTO', 'DIAGNOSTICO_CONCLUIDO') THEN 'AGUARDANDO_DIAGNOSTICO'
           WHEN os.status IN ('ORCAMENTO_GERADO', 'AGUARDANDO_APROVACAO') THEN 'AGUARDANDO_DECISAO'
           WHEN os.status = 'AGUARDANDO_EXECUCAO' THEN 'AGUARDANDO_EXECUCAO'
           WHEN os.status = 'SERVICO_EM_ANDAMENTO' THEN 'EM_EXECUCAO'
           WHEN os.motivo_encerramento = 'ORCAMENTO_RECUSADO' THEN 'ENCERRADA_SEM_EXECUCAO'
           ELSE 'CONCLUIDA'
       END,
       CASE
           WHEN os.status IN ('OS_ABERTA', 'DIAGNOSTICO_EM_ANDAMENTO', 'DIAGNOSTICO_CONCLUIDO') THEN 'DIAGNOSTICO'
           WHEN os.status IN ('ORCAMENTO_GERADO', 'AGUARDANDO_APROVACAO') THEN 'DECISAO'
           WHEN os.status IN ('AGUARDANDO_EXECUCAO', 'SERVICO_EM_ANDAMENTO') THEN 'EXECUCAO'
           ELSE 'FIM'
       END,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
  FROM ordens_de_servico os
 WHERE NOT EXISTS (SELECT 1 FROM saga_instancia s WHERE s.ordem_de_servico_id = os.id);

INSERT INTO saga_passos (saga_id, passo, tipo, detalhe, ocorrido_em)
SELECT s.id, s.passo_atual, 'ADOCAO', 'Saga criada pela migration V23 para OS existente', CURRENT_TIMESTAMP
  FROM saga_instancia s
 WHERE NOT EXISTS (SELECT 1 FROM saga_passos p WHERE p.saga_id = s.id);

UPDATE orcamentos o
   SET saga_id = s.id
  FROM saga_instancia s
 WHERE s.ordem_de_servico_id = o.ordem_de_servico_id
   AND o.saga_id IS NULL;
