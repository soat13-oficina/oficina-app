package br.com.oficina.pecainsumo.application.usecase;

import br.com.oficina.pecainsumo.application.command.AdicionarEstoquePecaCommand;

public interface AdicionarEstoquePecaUseCase {
    void adicionarEstoque(AdicionarEstoquePecaCommand command);
}
