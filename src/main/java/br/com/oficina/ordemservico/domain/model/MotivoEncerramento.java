package br.com.oficina.ordemservico.domain.model;

/**
 * Distingue os desfechos que compartilham a situação "Finalizada" (FR-011a): serviço concluído,
 * orçamento recusado ou (Fase 4) orçamento sem decisão dentro do prazo. Na Fase 4 também diz por que
 * uma OS foi "Cancelada" pela Saga: pagamento recusado, pagamento expirado ou execução abortada.
 */
public enum MotivoEncerramento {
    SERVICO_CONCLUIDO,
    ORCAMENTO_RECUSADO,
    /** Fase 4: o prazo de decisão da Saga venceu sem resposta do cliente. */
    ORCAMENTO_EXPIRADO,
    /** Fase 4: o cliente nao conseguiu pagar (cartao recusado). OS "Cancelada". */
    PAGAMENTO_RECUSADO,
    /** Fase 4: o prazo de pagamento venceu. OS "Cancelada". */
    PAGAMENTO_EXPIRADO,
    /** Fase 4: o mecanico abortou a execucao; pagamento estornado. OS "Cancelada". */
    FALHA_EXECUCAO
}
