-- Marca quando a SITUACAO de negocio (SituacaoOrdemDeServico) mudou pela ultima vez.
--
-- Nao e o mesmo que "quando o status mudou": DIAGNOSTICO_EM_ANDAMENTO e
-- DIAGNOSTICO_CONCLUIDO sao dois status dentro da MESMA situacao (Diagnostico), e o
-- relogio nao pode reiniciar no meio dela. O agregado so toca esta coluna quando
-- SituacaoOrdemDeServico.fromStatus() muda de valor.
--
-- Existe para o dashboard "tempo medio de execucao por status": iniciada_em /
-- finalizada_em / entregue_em sozinhos nao dizem quanto tempo a OS ficou EM
-- Diagnostico ou EM Execucao, ainda mais porque finalizada_em e escrito por tres
-- caminhos diferentes (concluirServico, recusarOrcamento e finalizar).
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
          AND column_name = 'situacao_alterada_em'
    ) THEN
        EXECUTE 'ALTER TABLE public.ordens_de_servico ADD COLUMN situacao_alterada_em TIMESTAMP';
    END IF;

    -- Backfill do melhor timestamp conhecido para a situacao ATUAL de cada ordem.
    -- Ordens em OS_ABERTA ficam NULL de proposito: a tabela nunca teve coluna de
    -- criacao, entao nao ha o que preencher. O listener de metricas trata NULL
    -- pulando o timer, em vez de inventar duracao.
    EXECUTE $backfill$
        UPDATE public.ordens_de_servico
           SET situacao_alterada_em = COALESCE(entregue_em, finalizada_em, iniciada_em)
         WHERE situacao_alterada_em IS NULL
    $backfill$;
END $$;
