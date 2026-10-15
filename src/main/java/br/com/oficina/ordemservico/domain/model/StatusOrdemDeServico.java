package br.com.oficina.ordemservico.domain.model;

public enum StatusOrdemDeServico {
    OS_ABERTA,
    DIAGNOSTICO_EM_ANDAMENTO,
    DIAGNOSTICO_CONCLUIDO,
    ORCAMENTO_GERADO,
    AGUARDANDO_APROVACAO,
    /** Fase 4: orcamento aprovado, na fila de execucao (a Saga leva a OS ate aqui). */
    AGUARDANDO_EXECUCAO,
    SERVICO_EM_ANDAMENTO,
    OS_FINALIZADA,
    ENTREGUE
}
