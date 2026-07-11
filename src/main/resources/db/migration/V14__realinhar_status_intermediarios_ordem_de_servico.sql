UPDATE ordens_de_servico
   SET status = 'DIAGNOSTICO_CONCLUIDO'
 WHERE status IN ('AGUARDANDO_ORCAMENTO', 'ORCAMENTO_GERADO');

UPDATE ordens_de_servico
   SET status = 'SERVICO_EM_ANDAMENTO'
 WHERE status IN ('ORCAMENTO_APROVADO', 'AGUARDANDO_PECA');

UPDATE ordens_de_servico
   SET status = 'OS_FINALIZADA',
       motivo_encerramento = COALESCE(motivo_encerramento, 'SERVICO_CONCLUIDO')
 WHERE status = 'SERVICO_CONCLUIDO';
