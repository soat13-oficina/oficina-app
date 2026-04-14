UPDATE ordens_de_servico
SET status = 'OS_ABERTA'
WHERE status = 'ABERTA';

UPDATE ordens_de_servico
SET status = 'OS_FINALIZADA'
WHERE status = 'FINALIZADA';
