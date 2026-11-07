package br.com.oficina.ordemservico.domain.model;

public enum StatusOrdemDeServico {
    OS_ABERTA,
    DIAGNOSTICO_EM_ANDAMENTO,
    DIAGNOSTICO_CONCLUIDO,
    ORCAMENTO_GERADO,
    AGUARDANDO_APROVACAO,
    /** Fase 4: orcamento aprovado e pecas reservadas; falta o cliente pagar. */
    AGUARDANDO_PAGAMENTO,
    /** Fase 4: pagamento confirmado, na fila de execucao (a Saga leva a OS ate aqui). */
    AGUARDANDO_EXECUCAO,
    SERVICO_EM_ANDAMENTO,
    OS_FINALIZADA,
    ENTREGUE,
    /** Fase 4: desfeita por compensacao da Saga (pagamento recusado/expirado, execucao abortada). */
    CANCELADA
}
