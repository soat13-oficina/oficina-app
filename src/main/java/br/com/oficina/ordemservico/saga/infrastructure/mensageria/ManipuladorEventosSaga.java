package br.com.oficina.ordemservico.saga.infrastructure.mensageria;

import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.ManipuladorMensagem;
import br.com.oficina.ordemservico.saga.application.OrquestradorSagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.MensagensSaga;

/** Eventos dos participantes ({@code os-saga-replies}): todos vao para o orquestrador. */
@Component
public class ManipuladorEventosSaga implements ManipuladorMensagem {

    private final OrquestradorSagaOrdemServico orquestrador;

    public ManipuladorEventosSaga(OrquestradorSagaOrdemServico orquestrador) {
        this.orquestrador = orquestrador;
    }

    @Override
    public Set<String> tipos() {
        return Set.copyOf(MensagensSaga.EVENTOS);
    }

    @Override
    public void tratar(Envelope envelope) {
        orquestrador.tratar(envelope);
    }
}
