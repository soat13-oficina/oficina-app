package br.com.oficina.veiculo.application.usecase;

import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;

public interface ExcluirVeiculoUseCase {
    void excluirVeiculo(ExcluirVeiculoCommand command);
}
