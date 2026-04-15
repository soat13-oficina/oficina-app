DO $$
DECLARE
    constraint_name text;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'orcamentos'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamentos'
              AND column_name = 'desconto'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamentos ADD COLUMN desconto NUMERIC(19, 2)';
        END IF;

        EXECUTE 'UPDATE public.orcamentos SET desconto = 0 WHERE desconto IS NULL';
        EXECUTE 'ALTER TABLE public.orcamentos ALTER COLUMN desconto SET NOT NULL';

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamentos'
              AND column_name = 'ordem_de_servico_id'
              AND data_type <> 'uuid'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamentos ADD COLUMN ordem_de_servico_uuid UUID';
            EXECUTE $update_orcamento_ordem_id$
                UPDATE public.orcamentos o
                   SET ordem_de_servico_uuid = CASE
                        WHEN o.ordem_de_servico_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
                            THEN o.ordem_de_servico_id::uuid
                        ELSE NULL
                   END
            $update_orcamento_ordem_id$;

            IF EXISTS (
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'ordens_de_servico'
            ) THEN
                EXECUTE $match_ordem_numero$
                    UPDATE public.orcamentos o
                       SET ordem_de_servico_uuid = os.id
                      FROM public.ordens_de_servico os
                     WHERE o.ordem_de_servico_uuid IS NULL
                       AND o.ordem_de_servico_id = os.numero_ordem_servico
                $match_ordem_numero$;
            END IF;

            EXECUTE 'ALTER TABLE public.orcamentos DROP COLUMN ordem_de_servico_id';
            EXECUTE 'ALTER TABLE public.orcamentos RENAME COLUMN ordem_de_servico_uuid TO ordem_de_servico_id';
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamentos'
              AND column_name = 'funcionario_id'
              AND data_type <> 'uuid'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamentos ADD COLUMN funcionario_uuid UUID';
            EXECUTE $update_orcamento_funcionario_id$
                UPDATE public.orcamentos
                   SET funcionario_uuid = CASE
                        WHEN funcionario_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
                            THEN funcionario_id::uuid
                        ELSE NULL
                   END
            $update_orcamento_funcionario_id$;
            EXECUTE 'ALTER TABLE public.orcamentos DROP COLUMN funcionario_id';
            EXECUTE 'ALTER TABLE public.orcamentos RENAME COLUMN funcionario_uuid TO funcionario_id';
        END IF;

        EXECUTE 'ALTER TABLE public.orcamentos ALTER COLUMN ordem_de_servico_id SET NOT NULL';
        EXECUTE 'ALTER TABLE public.orcamentos ALTER COLUMN funcionario_id SET NOT NULL';

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
           WHERE nsp.nspname = 'public'
             AND rel.relname = 'orcamentos'
             AND con.conname = 'fk_orcamentos_cliente_id'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamentos ADD CONSTRAINT fk_orcamentos_cliente_id
                FOREIGN KEY (cliente_id) REFERENCES public.clientes(id)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
           WHERE nsp.nspname = 'public'
             AND rel.relname = 'orcamentos'
             AND con.conname = 'fk_orcamentos_ordem_de_servico_id'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamentos ADD CONSTRAINT fk_orcamentos_ordem_de_servico_id
                FOREIGN KEY (ordem_de_servico_id) REFERENCES public.ordens_de_servico(id)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
           WHERE nsp.nspname = 'public'
             AND rel.relname = 'orcamentos'
             AND con.conname = 'fk_orcamentos_funcionario_id'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamentos ADD CONSTRAINT fk_orcamentos_funcionario_id
                FOREIGN KEY (funcionario_id) REFERENCES public.funcionarios(id)';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'orcamento_pecas_previstas'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamento_pecas_previstas'
              AND column_name = 'peca'
        ) AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamento_pecas_previstas'
              AND column_name = 'descricao'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas RENAME COLUMN peca TO descricao';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamento_pecas_previstas'
              AND column_name = 'descricao'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ADD COLUMN descricao VARCHAR(255)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamento_pecas_previstas'
              AND column_name = 'preco'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ADD COLUMN preco NUMERIC(19, 2)';
        END IF;

        EXECUTE 'UPDATE public.orcamento_pecas_previstas SET preco = 0 WHERE preco IS NULL';
        EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ALTER COLUMN descricao SET NOT NULL';
        EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ALTER COLUMN preco SET NOT NULL';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'veiculos'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
           WHERE nsp.nspname = 'public'
             AND rel.relname = 'veiculos'
             AND con.conname = 'fk_veiculos_cliente_id'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD CONSTRAINT fk_veiculos_cliente_id
                FOREIGN KEY (cliente_id) REFERENCES public.clientes(id)';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
           WHERE nsp.nspname = 'public'
             AND rel.relname = 'ordens_de_servico'
             AND con.conname = 'fk_ordens_de_servico_cliente_id'
        ) THEN
            EXECUTE 'ALTER TABLE public.ordens_de_servico ADD CONSTRAINT fk_ordens_de_servico_cliente_id
                FOREIGN KEY (cliente_id) REFERENCES public.clientes(id)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
           WHERE nsp.nspname = 'public'
             AND rel.relname = 'ordens_de_servico'
             AND con.conname = 'fk_ordens_de_servico_veiculo_id'
        ) THEN
            EXECUTE 'ALTER TABLE public.ordens_de_servico ADD CONSTRAINT fk_ordens_de_servico_veiculo_id
                FOREIGN KEY (veiculo_id) REFERENCES public.veiculos(id)';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'orcamentos'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE schemaname = 'public'
              AND tablename = 'orcamentos'
              AND indexname = 'idx_orcamentos_ordem_de_servico_id'
        ) THEN
            EXECUTE 'CREATE INDEX idx_orcamentos_ordem_de_servico_id ON public.orcamentos(ordem_de_servico_id)';
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE schemaname = 'public'
              AND tablename = 'orcamentos'
              AND indexname = 'idx_orcamentos_funcionario_id'
        ) THEN
            EXECUTE 'CREATE INDEX idx_orcamentos_funcionario_id ON public.orcamentos(funcionario_id)';
        END IF;
    END IF;
END $$;
