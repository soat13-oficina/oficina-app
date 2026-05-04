package br.com.oficina.pecainsumo.application.usecase;

import br.com.oficina.pecainsumo.application.command.ReservarPecaCommand;

public interface ReservarPecaUseCase {
    void reservarPeca(ReservarPecaCommand command);
}
