package br.com.oficina.ordemservico.saga.domain.model;

/** Passo de negocio em que a Saga esta (tabela de passos do ADR 0004). */
public enum PassoSaga {
    DIAGNOSTICO,
    ORCAMENTO,
    DECISAO,
    RESERVA,
    PAGAMENTO,
    EXECUCAO,
    COMPENSACAO,
    FIM
}
