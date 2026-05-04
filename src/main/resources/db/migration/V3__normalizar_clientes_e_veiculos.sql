DO $$
DECLARE
    constraint_name text;
    legacy_cliente_id boolean := false;
    legacy_veiculo_id boolean := false;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'clientes'
    ) THEN
        SELECT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'clientes'
              AND column_name = 'id'
              AND data_type <> 'uuid'
        )
        INTO legacy_cliente_id;

        IF legacy_cliente_id THEN
            EXECUTE 'ALTER TABLE public.clientes DROP COLUMN id';
            EXECUTE 'ALTER TABLE public.clientes ADD COLUMN id UUID';
        ELSIF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'clientes'
              AND column_name = 'id'
        ) THEN
            EXECUTE 'ALTER TABLE public.clientes ADD COLUMN id UUID';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'clientes'
              AND column_name = 'nome'
        ) THEN
            EXECUTE 'ALTER TABLE public.clientes ADD COLUMN nome VARCHAR(255)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'clientes'
              AND column_name = 'cpf_ou_cnpj'
        ) THEN
            EXECUTE 'ALTER TABLE public.clientes ADD COLUMN cpf_ou_cnpj VARCHAR(255)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'clientes'
              AND column_name = 'tipo_cliente'
        ) THEN
            EXECUTE 'ALTER TABLE public.clientes ADD COLUMN tipo_cliente VARCHAR(255)';
        END IF;

        EXECUTE $update_clientes$
            UPDATE public.clientes
               SET id = (
                    substr(md5(random()::text || clock_timestamp()::text), 1, 8) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 9, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 13, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 17, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 21, 12)
               )::uuid
             WHERE id IS NULL
        $update_clientes$;

        constraint_name := NULL;
        SELECT con.conname
          INTO constraint_name
          FROM pg_constraint con
          JOIN pg_class rel ON rel.oid = con.conrelid
          JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
         WHERE nsp.nspname = 'public'
           AND rel.relname = 'clientes'
           AND con.contype = 'p'
         LIMIT 1;
        IF constraint_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE public.clientes DROP CONSTRAINT %I', constraint_name);
        END IF;

        EXECUTE 'ALTER TABLE public.clientes ALTER COLUMN id SET NOT NULL';
        EXECUTE 'ALTER TABLE public.clientes ALTER COLUMN nome SET NOT NULL';

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
           WHERE nsp.nspname = 'public'
             AND rel.relname = 'clientes'
             AND con.conname = 'pk_clientes'
        ) THEN
            EXECUTE 'ALTER TABLE public.clientes ADD CONSTRAINT pk_clientes PRIMARY KEY (id)';
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'veiculos'
    ) THEN
        SELECT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'id'
              AND data_type <> 'uuid'
        )
        INTO legacy_veiculo_id;

        IF legacy_veiculo_id THEN
            EXECUTE 'ALTER TABLE public.veiculos DROP COLUMN id';
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN id UUID';
        ELSIF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'id'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN id UUID';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'cliente_id'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN cliente_id UUID';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'placa'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN placa VARCHAR(255)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'placa'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN placa VARCHAR(255)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'marca'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN marca VARCHAR(255)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'modelo'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN modelo VARCHAR(255)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'fabricante'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN fabricante VARCHAR(255)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'ano'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN ano INTEGER';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'potencia'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN potencia INTEGER';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'cambio'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN cambio VARCHAR(255)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'veiculos'
              AND column_name = 'tipo'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD COLUMN tipo VARCHAR(255)';
        END IF;

        EXECUTE $update_veiculos$
            UPDATE public.veiculos
               SET id = (
                    substr(md5(random()::text || clock_timestamp()::text), 1, 8) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 9, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 13, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 17, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 21, 12)
               )::uuid
             WHERE id IS NULL
        $update_veiculos$;

        constraint_name := NULL;
        SELECT con.conname
          INTO constraint_name
          FROM pg_constraint con
          JOIN pg_class rel ON rel.oid = con.conrelid
          JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
         WHERE nsp.nspname = 'public'
           AND rel.relname = 'veiculos'
           AND con.contype = 'p'
         LIMIT 1;
        IF constraint_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE public.veiculos DROP CONSTRAINT %I', constraint_name);
        END IF;

        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN id SET NOT NULL';
        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN cliente_id SET NOT NULL';
        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN placa SET NOT NULL';
        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN marca SET NOT NULL';
        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN modelo SET NOT NULL';
        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN fabricante SET NOT NULL';
        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN ano SET NOT NULL';
        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN potencia SET NOT NULL';
        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN cambio SET NOT NULL';
        EXECUTE 'ALTER TABLE public.veiculos ALTER COLUMN tipo SET NOT NULL';

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
           WHERE nsp.nspname = 'public'
             AND rel.relname = 'veiculos'
             AND con.conname = 'pk_veiculos'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD CONSTRAINT pk_veiculos PRIMARY KEY (id)';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
           WHERE nsp.nspname = 'public'
             AND rel.relname = 'veiculos'
             AND con.conname = 'uk_veiculos_placa'
        ) THEN
            EXECUTE 'ALTER TABLE public.veiculos ADD CONSTRAINT uk_veiculos_placa UNIQUE (placa)';
        END IF;
    END IF;
END $$;
