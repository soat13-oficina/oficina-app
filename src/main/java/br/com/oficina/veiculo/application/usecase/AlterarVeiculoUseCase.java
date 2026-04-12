package br.com.oficina.veiculo.application.usecase;

import br.com.oficina.veiculo.application.command.AlterarVeiculoCommand;

public interface AlterarVeiculoUseCase {
    void alterarVeiculo(AlterarVeiculoCommand command);
}
