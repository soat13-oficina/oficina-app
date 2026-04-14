CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS clientes (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cpf_ou_cnpj VARCHAR(255),
    tipo_cliente VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS veiculos (
    id UUID PRIMARY KEY,
    cliente_id UUID NOT NULL,
    placa VARCHAR(255) NOT NULL UNIQUE,
    marca VARCHAR(255) NOT NULL,
    modelo VARCHAR(255) NOT NULL,
    fabricante VARCHAR(255) NOT NULL,
    ano INTEGER NOT NULL,
    potencia INTEGER NOT NULL,
    cambio VARCHAR(255) NOT NULL,
    tipo VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS funcionarios (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS orcamentos (
    id UUID PRIMARY KEY,
    numero_orcamento VARCHAR(255) NOT NULL UNIQUE,
    cliente_id UUID NOT NULL,
    ordem_de_servico_id VARCHAR(255) NOT NULL,
    funcionario_id VARCHAR(255) NOT NULL,
    cliente_nome VARCHAR(255) NOT NULL,
    cliente_cpf VARCHAR(255) NOT NULL,
    placa_veiculo VARCHAR(255) NOT NULL,
    marca_veiculo VARCHAR(255) NOT NULL,
    modelo_veiculo VARCHAR(255) NOT NULL,
    descricao_diagnostico VARCHAR(4000) NOT NULL,
    valor_mao_de_obra NUMERIC(19, 2) NOT NULL,
    valor_pecas NUMERIC(19, 2) NOT NULL,
    valor_total NUMERIC(19, 2) NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL,
    validade TIMESTAMP(6) NOT NULL,
    observacoes VARCHAR(4000),
    status VARCHAR(255) NOT NULL,
    enviado_para_aprovacao_em TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS orcamento_servicos_propostos (
    orcamento_id UUID NOT NULL,
    servico VARCHAR(255) NOT NULL,
    CONSTRAINT fk_orcamento_servicos_propostos_orcamento_id
        FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id)
);

CREATE TABLE IF NOT EXISTS orcamento_pecas_previstas (
    orcamento_id UUID NOT NULL,
    peca VARCHAR(255) NOT NULL,
    CONSTRAINT fk_orcamento_pecas_previstas_orcamento_id
        FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id)
);

CREATE TABLE IF NOT EXISTS ordens_de_servico (
    id UUID PRIMARY KEY,
    numero_ordem_servico VARCHAR(255) NOT NULL UNIQUE,
    funcionario_id UUID NOT NULL,
    funcionario_nome VARCHAR(255) NOT NULL,
    funcionario_cpf VARCHAR(255),
    cliente_id UUID NOT NULL,
    veiculo_id UUID NOT NULL,
    cliente_nome VARCHAR(255) NOT NULL,
    cliente_documento VARCHAR(255),
    cliente_tipo VARCHAR(255),
    veiculo_placa VARCHAR(255) NOT NULL,
    veiculo_marca VARCHAR(255) NOT NULL,
    veiculo_modelo VARCHAR(255) NOT NULL,
    veiculo_fabricante VARCHAR(255) NOT NULL,
    veiculo_ano INTEGER NOT NULL,
    veiculo_potencia INTEGER NOT NULL,
    veiculo_cambio VARCHAR(255) NOT NULL,
    veiculo_tipo VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    iniciada_em TIMESTAMP,
    finalizada_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pecas_insumos (
    id VARCHAR(255) PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    marca VARCHAR(255) NOT NULL,
    preco NUMERIC(19, 2) NOT NULL,
    quantidade_estoque INTEGER NOT NULL,
    quantidade_reservada INTEGER NOT NULL,
    codigo_referencia VARCHAR(255) NOT NULL,
    categoria VARCHAR(255) NOT NULL
);
