DO $$
DECLARE
    constraint_name text;
    legacy_orcamento_id boolean := false;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'orcamentos'
    ) THEN
        RETURN;
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'orcamentos'
          AND column_name = 'id'
          AND data_type <> 'uuid'
    )
    INTO legacy_orcamento_id;

    IF NOT legacy_orcamento_id THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'orcamentos'
          AND column_name = 'numero_orcamento'
    ) THEN
        EXECUTE 'ALTER TABLE public.orcamentos RENAME COLUMN id TO numero_orcamento';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'orcamentos'
          AND column_name = 'id'
    ) THEN
        EXECUTE 'ALTER TABLE public.orcamentos ADD COLUMN id UUID';
    END IF;

    EXECUTE $update_orcamentos$
        UPDATE public.orcamentos
           SET id = (
                substr(md5(random()::text || clock_timestamp()::text), 1, 8) || '-' ||
                substr(md5(random()::text || clock_timestamp()::text), 9, 4) || '-' ||
                substr(md5(random()::text || clock_timestamp()::text), 13, 4) || '-' ||
                substr(md5(random()::text || clock_timestamp()::text), 17, 4) || '-' ||
                substr(md5(random()::text || clock_timestamp()::text), 21, 12)
           )::uuid
         WHERE id IS NULL
    $update_orcamentos$;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'orcamento_servicos_propostos'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamento_servicos_propostos'
              AND column_name = 'orcamento_uuid'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamento_servicos_propostos ADD COLUMN orcamento_uuid UUID';
        END IF;

        EXECUTE $update_servicos$
            UPDATE public.orcamento_servicos_propostos osp
               SET orcamento_uuid = o.id
              FROM public.orcamentos o
             WHERE osp.orcamento_uuid IS NULL
               AND osp.orcamento_id = o.numero_orcamento
        $update_servicos$;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'orcamento_pecas_previstas'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamento_pecas_previstas'
              AND column_name = 'orcamento_uuid'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ADD COLUMN orcamento_uuid UUID';
        END IF;

        EXECUTE $update_pecas$
            UPDATE public.orcamento_pecas_previstas opp
               SET orcamento_uuid = o.id
              FROM public.orcamentos o
             WHERE opp.orcamento_uuid IS NULL
               AND opp.orcamento_id = o.numero_orcamento
        $update_pecas$;
    END IF;

    SELECT con.conname
      INTO constraint_name
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
     WHERE nsp.nspname = 'public'
       AND rel.relname = 'orcamento_servicos_propostos'
       AND con.contype = 'f'
     LIMIT 1;
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.orcamento_servicos_propostos DROP CONSTRAINT %I', constraint_name);
    END IF;

    constraint_name := NULL;
    SELECT con.conname
      INTO constraint_name
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
     WHERE nsp.nspname = 'public'
       AND rel.relname = 'orcamento_pecas_previstas'
       AND con.contype = 'f'
     LIMIT 1;
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.orcamento_pecas_previstas DROP CONSTRAINT %I', constraint_name);
    END IF;

    constraint_name := NULL;
    SELECT con.conname
      INTO constraint_name
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
     WHERE nsp.nspname = 'public'
       AND rel.relname = 'orcamentos'
       AND con.contype = 'p'
     LIMIT 1;
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.orcamentos DROP CONSTRAINT %I', constraint_name);
    END IF;

    EXECUTE 'ALTER TABLE public.orcamentos ALTER COLUMN id SET NOT NULL';

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
       WHERE nsp.nspname = 'public'
         AND rel.relname = 'orcamentos'
         AND con.conname = 'pk_orcamentos'
    ) THEN
        EXECUTE 'ALTER TABLE public.orcamentos ADD CONSTRAINT pk_orcamentos PRIMARY KEY (id)';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
       WHERE nsp.nspname = 'public'
         AND rel.relname = 'orcamentos'
         AND con.conname = 'uk_orcamentos_numero_orcamento'
    ) THEN
        EXECUTE 'ALTER TABLE public.orcamentos ADD CONSTRAINT uk_orcamentos_numero_orcamento UNIQUE (numero_orcamento)';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'orcamento_servicos_propostos'
          AND column_name = 'orcamento_uuid'
    ) THEN
        EXECUTE 'ALTER TABLE public.orcamento_servicos_propostos DROP COLUMN orcamento_id';
        EXECUTE 'ALTER TABLE public.orcamento_servicos_propostos RENAME COLUMN orcamento_uuid TO orcamento_id';
        EXECUTE 'ALTER TABLE public.orcamento_servicos_propostos ALTER COLUMN orcamento_id SET NOT NULL';
        EXECUTE 'ALTER TABLE public.orcamento_servicos_propostos ADD CONSTRAINT fk_orcamento_servicos_propostos_orcamento_id FOREIGN KEY (orcamento_id) REFERENCES public.orcamentos(id)';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'orcamento_pecas_previstas'
          AND column_name = 'orcamento_uuid'
    ) THEN
        EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas DROP COLUMN orcamento_id';
        EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas RENAME COLUMN orcamento_uuid TO orcamento_id';
        EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ALTER COLUMN orcamento_id SET NOT NULL';
        EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ADD CONSTRAINT fk_orcamento_pecas_previstas_orcamento_id FOREIGN KEY (orcamento_id) REFERENCES public.orcamentos(id)';
    END IF;
END $$;
