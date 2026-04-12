package br.com.oficina.pecainsumo.application.usecase;

import br.com.oficina.pecainsumo.application.command.LiberarReservaPecaCommand;

public interface LiberarReservaPecaUseCase {
    void liberarReserva(LiberarReservaPecaCommand command);
}
