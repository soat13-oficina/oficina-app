package br.com.oficina.ordemservico.saga.application;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

/**
 * O que o OS Service avisa a Saga: so a abertura da ordem. Dali em diante quem move a ordem sao os
 * eventos da Execution (diagnostico e execucao, feitos pelo mecanico) e do Billing, e a Saga reage a eles
 * ({@code OrquestradorSagaOrdemServico#tratar}).
 */
public interface PassosDaSaga {

    /** Para os testes de unidade dos casos de uso que nao exercitam a Saga. */
    PassosDaSaga NENHUM = ordem -> {
    };

    /**
     * A ordem foi aberta: nasce a Saga e a OS entra na fila de diagnostico da Execution
     * ({@code EnfileirarDiagnostico}, gravado no outbox na mesma transacao).
     */
    void iniciar(OrdemDeServico ordem);
}
