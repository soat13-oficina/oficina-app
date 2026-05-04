DO $$
BEGIN
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
              AND column_name = 'peca_insumo_id'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ADD COLUMN peca_insumo_id VARCHAR(255)';
            EXECUTE 'UPDATE public.orcamento_pecas_previstas SET peca_insumo_id = ''LEGADO'' WHERE peca_insumo_id IS NULL';
            EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ALTER COLUMN peca_insumo_id SET NOT NULL';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'orcamento_pecas_previstas'
              AND column_name = 'quantidade'
        ) THEN
            EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ADD COLUMN quantidade INTEGER';
            EXECUTE 'UPDATE public.orcamento_pecas_previstas SET quantidade = 1 WHERE quantidade IS NULL';
            EXECUTE 'ALTER TABLE public.orcamento_pecas_previstas ALTER COLUMN quantidade SET NOT NULL';
        END IF;
    END IF;
END $$;
