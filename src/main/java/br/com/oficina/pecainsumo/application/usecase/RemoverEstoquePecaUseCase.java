package br.com.oficina.pecainsumo.application.usecase;

import br.com.oficina.pecainsumo.application.command.RemoverEstoquePecaCommand;

public interface RemoverEstoquePecaUseCase {
    void removerEstoque(RemoverEstoquePecaCommand command);
}
