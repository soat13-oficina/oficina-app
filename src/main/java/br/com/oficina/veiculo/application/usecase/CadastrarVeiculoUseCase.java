package br.com.oficina.veiculo.application.usecase;

import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;

public interface CadastrarVeiculoUseCase {
    void cadastrarVeiculo(CadastrarVeiculoCommand command);
}
