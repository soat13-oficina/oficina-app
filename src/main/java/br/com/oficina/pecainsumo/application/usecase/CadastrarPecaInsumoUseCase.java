package br.com.oficina.pecainsumo.application.usecase;

import br.com.oficina.pecainsumo.application.command.CadastrarPecaInsumoCommand;

public interface CadastrarPecaInsumoUseCase {
    void cadastrarPecaInsumo(CadastrarPecaInsumoCommand command);
}
