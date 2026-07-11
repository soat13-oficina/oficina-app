-- Garante a unicidade do documento (CPF/CNPJ) dos clientes (spec 008 / CRI-002).
-- Guarda de precondicao: aborta com mensagem descritiva se houver documentos duplicados pre-existentes
-- (clientes sao entidades distintas e nao podem ser mesclados automaticamente; a limpeza e manual).
DO $$
DECLARE
    total_duplicados INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_duplicados FROM (
        SELECT cpf_ou_cnpj
          FROM clientes
         WHERE cpf_ou_cnpj IS NOT NULL
         GROUP BY cpf_ou_cnpj
        HAVING COUNT(*) > 1
    ) AS duplicados;

    IF total_duplicados > 0 THEN
        RAISE EXCEPTION 'Migracao V16 abortada: % documento(s) duplicado(s) em clientes.cpf_ou_cnpj. Resolva os duplicados antes de aplicar a constraint UNIQUE.', total_duplicados;
    END IF;
END $$;

ALTER TABLE clientes ADD CONSTRAINT uk_clientes_cpf_ou_cnpj UNIQUE (cpf_ou_cnpj);
