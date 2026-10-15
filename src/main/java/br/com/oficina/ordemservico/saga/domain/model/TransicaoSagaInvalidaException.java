package br.com.oficina.ordemservico.saga.domain.model;

/**
 * Evento ou acao que nao cabe no estado atual da Saga - por exemplo, {@code OrcamentoAprovado}
 * chegando depois de o prazo de decisao ter vencido. O orquestrador registra como
 * {@code EVENTO_IGNORADO} em vez de falhar a mensagem (ADR 0004, regra 3).
 */
public class TransicaoSagaInvalidaException extends RuntimeException {

    public TransicaoSagaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
