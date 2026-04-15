package br.com.oficina.pecainsumo.application.usecase;

import br.com.oficina.pecainsumo.application.command.ConsumirPecaCommand;

public interface ConsumirPecaUseCase {
    void consumirPeca(ConsumirPecaCommand command);
}
