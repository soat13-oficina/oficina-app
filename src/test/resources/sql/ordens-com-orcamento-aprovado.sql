-- Quatro OSs com orcamento (V1-V23): aprovada aguardando execucao (com peca repetida e peca que
-- nao existe mais), aprovada em execucao, aguardando aprovacao e finalizada. Usado para testar o
-- backfill de reservas_pecas da V24 e os repositorios de pagamento/reserva no esquema do Flyway.
INSERT INTO clientes (id, nome, cpf_ou_cnpj, tipo_cliente) VALUES ('10000000-0000-0000-0000-000000000001', 'Maria', '52998224725', 'PF');
INSERT INTO funcionarios (id, nome) VALUES ('20000000-0000-0000-0000-000000000001', 'Ana');
INSERT INTO veiculos (id, cliente_id, placa, marca, modelo, fabricante, ano, potencia, cambio, tipo)
VALUES ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'ABC1D23', 'Toyota', 'Corolla', 'Toyota', 2020, 177, 'AUTOMATICO', 'FLEX');
INSERT INTO pecas_insumos (id, descricao, marca, preco, quantidade_estoque, quantidade_reservada, codigo_referencia, categoria) VALUES
  ('filtro', 'Filtro', 'Bosch', 80, 10, 4, 'REF-1', 'FILTROS'),
  ('oleo', 'Oleo', 'Mobil', 45, 10, 4, 'REF-2', 'FILTROS');
INSERT INTO ordens_de_servico (id, numero_ordem_servico, funcionario_id, funcionario_nome, cliente_id, veiculo_id, cliente_nome,
  veiculo_placa, veiculo_marca, veiculo_modelo, veiculo_fabricante, veiculo_ano, veiculo_potencia, veiculo_cambio, veiculo_tipo, status)
SELECT ('4000000' || n || '-0000-0000-0000-000000000001')::uuid, 'OS-V24-' || n, '20000000-0000-0000-0000-000000000001', 'Ana',
  '10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'Maria',
  'ABC1D23', 'Toyota', 'Corolla', 'Toyota', 2020, 177, 'AUTOMATICO', 'FLEX', s
FROM (VALUES (1, 'AGUARDANDO_EXECUCAO'), (2, 'SERVICO_EM_ANDAMENTO'), (3, 'AGUARDANDO_APROVACAO'), (4, 'OS_FINALIZADA')) AS t(n, s);
INSERT INTO orcamentos (id, numero_orcamento, cliente_id, cliente_nome, cliente_cpf, placa_veiculo, marca_veiculo, modelo_veiculo,
  descricao_diagnostico, valor_mao_de_obra, valor_pecas, valor_total, criado_em, validade, status, desconto, ordem_de_servico_id, funcionario_id)
SELECT ('5000000' || n || '-0000-0000-0000-000000000001')::uuid, 'ORC-V24-' || n, '10000000-0000-0000-0000-000000000001', 'Maria',
  '52998224725', 'ABC1D23', 'Toyota', 'Corolla', 'Troca de oleo', 100, 0, 100, now(), now() + interval '7 days', s, 0,
  ('4000000' || n || '-0000-0000-0000-000000000001')::uuid, '20000000-0000-0000-0000-000000000001'
FROM (VALUES (1, 'APROVADO'), (2, 'APROVADO'), (3, 'AGUARDANDO_APROVACAO'), (4, 'APROVADO')) AS t(n, s);
INSERT INTO orcamento_pecas_previstas (orcamento_id, descricao, preco, peca_insumo_id, quantidade) VALUES
  ('50000001-0000-0000-0000-000000000001', 'Filtro', 80, 'filtro', 2),
  ('50000001-0000-0000-0000-000000000001', 'Filtro', 80, 'filtro', 1),
  ('50000001-0000-0000-0000-000000000001', 'Oleo', 45, 'oleo', 4),
  ('50000001-0000-0000-0000-000000000001', 'Sumiu', 10, 'sumiu', 1),
  ('50000002-0000-0000-0000-000000000001', 'Filtro', 80, 'filtro', 1),
  ('50000003-0000-0000-0000-000000000001', 'Filtro', 80, 'filtro', 5),
  ('50000004-0000-0000-0000-000000000001', 'Filtro', 80, 'filtro', 1);
