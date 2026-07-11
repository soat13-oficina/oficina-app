package br.com.oficina.ordemservico.domain.model;

/**
 * Distingue os dois desfechos que compartilham a situação "Finalizada" (FR-011a):
 * serviço concluído ou orçamento recusado.
 */
public enum MotivoEncerramento {
    SERVICO_CONCLUIDO,
    ORCAMENTO_RECUSADO
}
