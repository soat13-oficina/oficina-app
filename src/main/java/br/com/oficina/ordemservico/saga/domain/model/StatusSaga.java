package br.com.oficina.ordemservico.saga.domain.model;

/** Estados da Saga da ordem de servico (ADR 0004). */
public enum StatusSaga {
    AGUARDANDO_DIAGNOSTICO,
    GERANDO_ORCAMENTO,
    AGUARDANDO_DECISAO,
    RESERVANDO_PECAS,
    REVERTENDO_APROVACAO,
    AGUARDANDO_PAGAMENTO,
    AGUARDANDO_EXECUCAO,
    EM_EXECUCAO,
    /** Aguardando as confirmacoes das compensacoes; o estado final fica em {@code desfecho}. */
    COMPENSANDO,
    CONCLUIDA,
    COMPENSADA,
    ENCERRADA_SEM_EXECUCAO,
    /** Compensacao esgotou as tentativas: exige acao manual (monitorado). */
    FALHA_COMPENSACAO;

    public boolean isFinal() {
        return this == CONCLUIDA || this == COMPENSADA || this == ENCERRADA_SEM_EXECUCAO || this == FALHA_COMPENSACAO;
    }
}
