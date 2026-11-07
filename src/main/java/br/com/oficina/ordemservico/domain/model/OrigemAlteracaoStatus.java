package br.com.oficina.ordemservico.domain.model;

/**
 * Quem provocou uma mudanca de status da ordem de servico, registrada no historico.
 *
 * <p>Com a Saga (ADR 0004) o status deixa de mudar so por requisicao HTTP: passa a mudar
 * tambem por eventos dos outros servicos e por prazos vencidos. O historico guarda a origem
 * para que "por que esta OS mudou de situacao" tenha resposta.
 */
public enum OrigemAlteracaoStatus {
    /** Requisicao HTTP de um usuario (atendente, mecanico, cliente). */
    API,
    /** Evento processado pelo orquestrador da Saga. */
    SAGA,
    /** Prazo da Saga vencido (decisao do cliente, pagamento). */
    TIMEOUT,
    /** Linha criada pela migration V21 para ordens anteriores ao historico. */
    MIGRACAO
}
