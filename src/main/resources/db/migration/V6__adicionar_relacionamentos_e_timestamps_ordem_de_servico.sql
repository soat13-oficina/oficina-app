DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
    ) THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
          AND column_name = 'veiculo_id'
    ) THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico ADD COLUMN veiculo_id UUID';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'veiculos'
    ) THEN
        EXECUTE $update_veiculo_id$
            UPDATE public.ordens_de_servico os
               SET veiculo_id = v.id
              FROM public.veiculos v
             WHERE os.veiculo_id IS NULL
               AND os.veiculo_placa = v.placa
        $update_veiculo_id$;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
          AND column_name = 'iniciada_em'
    ) THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico ADD COLUMN iniciada_em TIMESTAMP';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
          AND column_name = 'finalizada_em'
    ) THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico ADD COLUMN finalizada_em TIMESTAMP';
    END IF;

    EXECUTE 'ALTER TABLE public.ordens_de_servico ALTER COLUMN veiculo_id SET NOT NULL';
END $$;
