CREATE TABLE notificacoes (
    id UUID PRIMARY KEY,
    cliente_id UUID NOT NULL,
    numero_ordem_servico VARCHAR(255) NOT NULL,
    destinatario_email VARCHAR(255),
    assunto VARCHAR(255) NOT NULL,
    corpo VARCHAR(2000) NOT NULL,
    situacao VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL,
    ultima_tentativa_em TIMESTAMP
);

CREATE INDEX idx_notificacoes_status_tentativas ON notificacoes (status, tentativas);
