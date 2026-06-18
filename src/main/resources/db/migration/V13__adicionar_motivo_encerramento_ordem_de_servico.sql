-- Motivo de encerramento da Ordem de Serviço (FR-011a): distingue os dois desfechos
-- que compartilham a situação "Finalizada" (serviço concluído x orçamento recusado).

ALTER TABLE ordens_de_servico
    ADD COLUMN motivo_encerramento VARCHAR(30);
