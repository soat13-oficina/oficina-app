package br.com.oficina.veiculo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.ExcluirVeiculoUseCase;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class ExcluirVeiculoService implements ExcluirVeiculoUseCase {
    private final VeiculoRepository veiculoRepository;

    public ExcluirVeiculoService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public void excluirVeiculo(ExcluirVeiculoCommand command) {
        veiculoRepository.excluirPorPlaca(command.placa());
    }
}
