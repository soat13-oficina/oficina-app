package br.com.oficina.veiculo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.veiculo.application.command.AlterarVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.AlterarVeiculoUseCase;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;

@Service
public class AlterarVeiculoService implements AlterarVeiculoUseCase {
    private final VeiculoRepository veiculoRepository;

    public AlterarVeiculoService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public void alterarVeiculo(AlterarVeiculoCommand command) {
        Veiculo veiculo = veiculoRepository.buscarPorPlaca(command.placa())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado para a placa informada."));

        veiculo.alterar(
                command.marca(),
                command.modelo(),
                command.fabricante(),
                command.ano(),
                command.potencia(),
                command.cambio(),
                command.tipo());
        veiculoRepository.salvar(veiculo);
    }
}
