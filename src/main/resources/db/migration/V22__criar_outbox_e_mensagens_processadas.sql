-- Mensageria da Saga (ADR 0005): outbox transacional no produtor e idempotencia no consumidor.
--
-- outbox_mensagens: a mensagem e gravada NA MESMA TRANSACAO da mudanca de estado e publicada
-- depois por um agendamento. Sem isso, gravar no banco e publicar na fila nao seriam atomicos:
-- um pod caindo entre os dois deixaria a Saga travada ou recebendo algo que nao aconteceu.
-- E o mesmo padrao que o modulo notificacao ja usa para e-mail (tabela notificacoes).
CREATE TABLE IF NOT EXISTS outbox_mensagens (
    id            UUID         PRIMARY KEY,           -- = messageId do envelope
    fila_destino  VARCHAR(120) NOT NULL,
    tipo          VARCHAR(60)  NOT NULL,
    group_id      VARCHAR(60)  NOT NULL,              -- = ordemServicoId (MessageGroupId do SQS FIFO)
    payload       TEXT         NOT NULL,              -- envelope JSON completo
    criada_em     TIMESTAMP    NOT NULL,
    publicada_em  TIMESTAMP,
    tentativas    INT          NOT NULL DEFAULT 0,
    ultimo_erro   TEXT
);

-- So as pendentes interessam ao publicador; o indice parcial fica pequeno mesmo com o historico.
CREATE INDEX IF NOT EXISTS idx_outbox_mensagens_pendentes
    ON outbox_mensagens (criada_em) WHERE publicada_em IS NULL;

-- mensagens_processadas: o SQS entrega PELO MENOS uma vez. O consumidor grava o messageId aqui
-- na mesma transacao do efeito; se ja existe, a mensagem e duplicata e e descartada.
CREATE TABLE IF NOT EXISTS mensagens_processadas (
    mensagem_id   UUID        PRIMARY KEY,
    tipo          VARCHAR(60) NOT NULL,
    processada_em TIMESTAMP   NOT NULL
);
