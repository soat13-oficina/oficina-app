package br.com.oficina.ponte;

import java.util.UUID;

import org.springframework.stereotype.Component;

import br.com.oficina.ordemservico.application.port.ReservasDaOrdem;
import br.com.oficina.pecainsumo.application.usecase.ConsumirReservasDaOrdemUseCase;

/** Conclusao do servico (OS) baixando as pecas reservadas (estoque). Transitorio: ver {@code package-info}. */
@Component
public class ReservasDaOrdemNoEstoque implements ReservasDaOrdem {

    private final ConsumirReservasDaOrdemUseCase consumirReservas;

    public ReservasDaOrdemNoEstoque(ConsumirReservasDaOrdemUseCase consumirReservas) {
        this.consumirReservas = consumirReservas;
    }

    @Override
    public int consumir(UUID ordemDeServicoId) {
        return consumirReservas.consumirReservas(ordemDeServicoId);
    }
}
