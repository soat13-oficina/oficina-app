-- Serviços previstos e peças previstas capturados na abertura da Ordem de Serviço (US1).
-- Mapeados como @ElementCollection em OrdemDeServico (join column: ordem_de_servico_id).

CREATE TABLE ordem_servico_servicos (
    ordem_de_servico_id UUID NOT NULL REFERENCES ordens_de_servico (id) ON DELETE CASCADE,
    descricao VARCHAR(255) NOT NULL,
    valor_mao_de_obra NUMERIC(19, 2)
);

CREATE INDEX idx_ordem_servico_servicos_os ON ordem_servico_servicos (ordem_de_servico_id);

CREATE TABLE ordem_servico_pecas (
    ordem_de_servico_id UUID NOT NULL REFERENCES ordens_de_servico (id) ON DELETE CASCADE,
    peca_insumo_id VARCHAR(255) NOT NULL,
    quantidade INTEGER NOT NULL
);

CREATE INDEX idx_ordem_servico_pecas_os ON ordem_servico_pecas (ordem_de_servico_id);
