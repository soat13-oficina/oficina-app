package br.com.oficina.veiculo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.ExcluirVeiculoUseCase;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;

@Service
public class ExcluirVeiculoService implements ExcluirVeiculoUseCase {
    private final VeiculoRepository veiculoRepository;

    public ExcluirVeiculoService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public void excluirVeiculo(ExcluirVeiculoCommand command) {
        veiculoRepository.buscarPorPlaca(command.placa())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado para a placa informada."));
        veiculoRepository.excluirPorPlaca(command.placa());
    }
}
