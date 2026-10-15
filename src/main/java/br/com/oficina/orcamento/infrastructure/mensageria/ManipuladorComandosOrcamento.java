package br.com.oficina.orcamento.infrastructure.mensageria;

import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.ManipuladorMensagem;
import br.com.oficina.orcamento.application.saga.CancelarOrcamentoPelaSaga;
import br.com.oficina.orcamento.application.saga.MensagensOrcamento;

/** Comandos do orquestrador ({@code billing-commands}) que o modulo de orcamento ja atende. */
@Component
public class ManipuladorComandosOrcamento implements ManipuladorMensagem {

    private final CancelarOrcamentoPelaSaga cancelarOrcamento;

    public ManipuladorComandosOrcamento(CancelarOrcamentoPelaSaga cancelarOrcamento) {
        this.cancelarOrcamento = cancelarOrcamento;
    }

    @Override
    public Set<String> tipos() {
        return Set.of(MensagensOrcamento.CANCELAR_ORCAMENTO);
    }

    @Override
    public void tratar(Envelope envelope) {
        cancelarOrcamento.cancelar(envelope);
    }
}
