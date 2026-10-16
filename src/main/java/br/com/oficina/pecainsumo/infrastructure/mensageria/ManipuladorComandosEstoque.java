package br.com.oficina.pecainsumo.infrastructure.mensageria;

import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.ManipuladorMensagem;
import br.com.oficina.pecainsumo.application.saga.MensagensEstoque;
import br.com.oficina.pecainsumo.application.saga.ReservasPelaSaga;

/** Comandos do orquestrador ({@code execution-commands}) que o estoque atende no monolito. */
@Component
public class ManipuladorComandosEstoque implements ManipuladorMensagem {

    private final ReservasPelaSaga reservas;

    public ManipuladorComandosEstoque(ReservasPelaSaga reservas) {
        this.reservas = reservas;
    }

    @Override
    public Set<String> tipos() {
        return Set.of(MensagensEstoque.RESERVAR_PECAS, MensagensEstoque.LIBERAR_RESERVA);
    }

    @Override
    public void tratar(Envelope envelope) {
        if (MensagensEstoque.RESERVAR_PECAS.equals(envelope.type())) {
            reservas.reservar(envelope);
        } else {
            reservas.liberar(envelope);
        }
    }
}
