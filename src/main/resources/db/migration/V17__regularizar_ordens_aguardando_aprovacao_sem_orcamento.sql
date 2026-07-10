-- Regulariza Ordens de Serviço legadas que ficaram em AGUARDANDO_APROVACAO sem orçamento
-- associado (spec 012-fix-aprovacao-sem-orcamento). Essas OS foram geradas pelo caminho legado
-- (POST /ordens-servico/{n}/orcamento/enviar-aprovacao), removido nesta correção, que transicionava
-- a situação sem criar o orçamento — deixando a OS travada (nada a aprovar/rejeitar).
--
-- Ação: reverter essas OS para DIAGNOSTICO_CONCLUIDO, tornando-as aptas a seguir o fluxo único
-- (POST /ordens-servico/{n}/diagnostico/enviar-para-orcamento), que gera o orçamento.
--
-- Idempotente: após executada, o conjunto-alvo fica vazio; reexecuções não têm efeito.
-- OS com orçamento associado não são tocadas.
UPDATE ordens_de_servico
   SET status = 'DIAGNOSTICO_CONCLUIDO'
 WHERE status = 'AGUARDANDO_APROVACAO'
   AND id NOT IN (
       SELECT ordem_de_servico_id
         FROM orcamentos
        WHERE ordem_de_servico_id IS NOT NULL
   );
