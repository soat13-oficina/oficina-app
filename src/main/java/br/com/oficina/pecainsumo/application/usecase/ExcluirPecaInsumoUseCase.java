package br.com.oficina.pecainsumo.application.usecase;

import br.com.oficina.pecainsumo.application.command.ExcluirPecaInsumoCommand;

public interface ExcluirPecaInsumoUseCase {
    void excluirPecaInsumo(ExcluirPecaInsumoCommand command);
}
