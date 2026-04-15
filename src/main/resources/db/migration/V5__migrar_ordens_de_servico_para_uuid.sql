DO $$
DECLARE
    constraint_name text;
    legacy_ordem_id boolean := false;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
    ) THEN
        RETURN;
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
          AND column_name = 'id'
          AND data_type <> 'uuid'
    )
    INTO legacy_ordem_id;

    IF legacy_ordem_id THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico RENAME COLUMN id TO id_legado';
        EXECUTE 'ALTER TABLE public.ordens_de_servico ADD COLUMN id UUID';
        EXECUTE $update_ordens_id$
            UPDATE public.ordens_de_servico
               SET id = (
                    substr(md5(random()::text || clock_timestamp()::text), 1, 8) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 9, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 13, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 17, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 21, 12)
               )::uuid
             WHERE id IS NULL
        $update_ordens_id$;
        EXECUTE 'ALTER TABLE public.ordens_de_servico DROP COLUMN id_legado';
    ELSIF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
          AND column_name = 'id'
    ) THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico ADD COLUMN id UUID';
        EXECUTE $fill_ordens_id$
            UPDATE public.ordens_de_servico
               SET id = (
                    substr(md5(random()::text || clock_timestamp()::text), 1, 8) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 9, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 13, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 17, 4) || '-' ||
                    substr(md5(random()::text || clock_timestamp()::text), 21, 12)
               )::uuid
             WHERE id IS NULL
        $fill_ordens_id$;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
          AND column_name = 'cliente_id'
          AND data_type <> 'uuid'
    ) THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico ALTER COLUMN cliente_id TYPE UUID USING cliente_id::uuid';
    END IF;

    constraint_name := NULL;
    SELECT con.conname
      INTO constraint_name
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
     WHERE nsp.nspname = 'public'
       AND rel.relname = 'ordens_de_servico'
       AND con.contype = 'p'
     LIMIT 1;
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.ordens_de_servico DROP CONSTRAINT %I', constraint_name);
    END IF;

    EXECUTE 'ALTER TABLE public.ordens_de_servico ALTER COLUMN id SET NOT NULL';
    EXECUTE 'ALTER TABLE public.ordens_de_servico ALTER COLUMN cliente_id SET NOT NULL';

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
       WHERE nsp.nspname = 'public'
         AND rel.relname = 'ordens_de_servico'
         AND con.conname = 'pk_ordens_de_servico'
    ) THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico ADD CONSTRAINT pk_ordens_de_servico PRIMARY KEY (id)';
    END IF;
END $$;
