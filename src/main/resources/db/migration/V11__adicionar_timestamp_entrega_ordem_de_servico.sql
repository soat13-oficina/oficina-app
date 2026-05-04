DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'ordens_de_servico'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'ordens_de_servico'
              AND column_name = 'entregue_em'
        ) THEN
            EXECUTE 'ALTER TABLE public.ordens_de_servico ADD COLUMN entregue_em TIMESTAMP';
        END IF;
    END IF;
END $$;
