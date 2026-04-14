DO $$
DECLARE
    constraint_name text;
    legacy_funcionario_id boolean := false;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'funcionarios'
    ) THEN
        EXECUTE 'CREATE TABLE public.funcionarios (
            id UUID PRIMARY KEY,
            nome VARCHAR(255) NOT NULL,
            cpf VARCHAR(255)
        )';
    END IF;

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
          AND column_name = 'funcionario_id'
          AND data_type <> 'uuid'
    )
    INTO legacy_funcionario_id;

    IF legacy_funcionario_id THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico RENAME COLUMN funcionario_id TO funcionario_id_legado';
        EXECUTE 'ALTER TABLE public.ordens_de_servico ADD COLUMN funcionario_id UUID';

        EXECUTE '
            INSERT INTO public.funcionarios (id, nome, cpf)
            SELECT DISTINCT
                (
                    substr(md5(random()::text || clock_timestamp()::text), 1, 8) || ''-'' ||
                    substr(md5(random()::text || clock_timestamp()::text), 9, 4) || ''-'' ||
                    substr(md5(random()::text || clock_timestamp()::text), 13, 4) || ''-'' ||
                    substr(md5(random()::text || clock_timestamp()::text), 17, 4) || ''-'' ||
                    substr(md5(random()::text || clock_timestamp()::text), 21, 12)
                )::uuid,
                os.funcionario_nome,
                os.funcionario_cpf
            FROM public.ordens_de_servico os
            WHERE NOT EXISTS (
                SELECT 1
                FROM public.funcionarios f
                WHERE f.nome = os.funcionario_nome
                  AND coalesce(f.cpf, '''') = coalesce(os.funcionario_cpf, '''')
            )';

        EXECUTE '
            UPDATE public.ordens_de_servico os
               SET funcionario_id = f.id
              FROM public.funcionarios f
             WHERE os.funcionario_id IS NULL
               AND f.nome = os.funcionario_nome
               AND coalesce(f.cpf, '''') = coalesce(os.funcionario_cpf, '''')';

        EXECUTE 'ALTER TABLE public.ordens_de_servico DROP COLUMN funcionario_id_legado';
    ELSE
        EXECUTE '
            INSERT INTO public.funcionarios (id, nome, cpf)
            SELECT DISTINCT
                os.funcionario_id,
                os.funcionario_nome,
                os.funcionario_cpf
            FROM public.ordens_de_servico os
            WHERE os.funcionario_id IS NOT NULL
              AND NOT EXISTS (
                    SELECT 1
                    FROM public.funcionarios f
                    WHERE f.id = os.funcionario_id
              )';
    END IF;

    EXECUTE 'ALTER TABLE public.ordens_de_servico ALTER COLUMN funcionario_id SET NOT NULL';

    SELECT con.conname
      INTO constraint_name
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
     WHERE nsp.nspname = 'public'
       AND rel.relname = 'ordens_de_servico'
       AND con.conname = 'fk_ordens_de_servico_funcionario_id'
     LIMIT 1;

    IF constraint_name IS NULL THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico
            ADD CONSTRAINT fk_ordens_de_servico_funcionario_id
            FOREIGN KEY (funcionario_id) REFERENCES public.funcionarios(id)';
    END IF;
END $$;
