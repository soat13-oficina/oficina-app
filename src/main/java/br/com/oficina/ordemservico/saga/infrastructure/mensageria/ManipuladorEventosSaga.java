package br.com.oficina.ordemservico.saga.infrastructure.mensageria;

import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.ManipuladorMensagem;
import br.com.oficina.ordemservico.saga.application.OrquestradorSagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.MensagensSaga;

/** Eventos dos participantes ({@code os-saga-replies}) que o orquestrador ja trata. */
@Component
public class ManipuladorEventosSaga implements ManipuladorMensagem {

    private final OrquestradorSagaOrdemServico orquestrador;

    public ManipuladorEventosSaga(OrquestradorSagaOrdemServico orquestrador) {
        this.orquestrador = orquestrador;
    }

    @Override
    public Set<String> tipos() {
        return Set.of(MensagensSaga.ORCAMENTO_APROVADO, MensagensSaga.ORCAMENTO_REJEITADO, MensagensSaga.ORCAMENTO_CANCELADO);
    }

    @Override
    public void tratar(Envelope envelope) {
        orquestrador.tratar(envelope);
    }
}
