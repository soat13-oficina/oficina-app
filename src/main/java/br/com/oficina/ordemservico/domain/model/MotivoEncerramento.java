package br.com.oficina.ordemservico.domain.model;

/**
 * Distingue os desfechos que compartilham a situação "Finalizada" (FR-011a):
 * serviço concluído, orçamento recusado ou (Fase 4) orçamento sem decisão dentro do prazo.
 */
public enum MotivoEncerramento {
    SERVICO_CONCLUIDO,
    ORCAMENTO_RECUSADO,
    /** Fase 4: o prazo de decisão da Saga venceu sem resposta do cliente. */
    ORCAMENTO_EXPIRADO
}
