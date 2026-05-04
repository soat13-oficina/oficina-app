package br.com.oficina.pecainsumo.application.usecase;

import br.com.oficina.pecainsumo.application.command.AlterarPecaInsumoCommand;

public interface AlterarPecaInsumoUseCase {
    void alterarPecaInsumo(AlterarPecaInsumoCommand command);
}
