-- Tokens de integração externa para autenticação do webhook (spec 015).
-- O segredo em claro NUNCA é persistido — apenas o hash SHA-256.
CREATE TABLE tokens_integracao (
    id            UUID PRIMARY KEY,
    rotulo        VARCHAR(255) NOT NULL,
    hash_token    VARCHAR(128) NOT NULL UNIQUE,
    status        VARCHAR(20)  NOT NULL,
    criado_em     TIMESTAMP    NOT NULL,
    criado_por    VARCHAR(255) NOT NULL,
    revogado_em   TIMESTAMP,
    revogado_por  VARCHAR(255)
);

CREATE INDEX idx_tokens_integracao_hash ON tokens_integracao (hash_token);
