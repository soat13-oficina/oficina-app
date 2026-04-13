package br.com.oficina.veiculo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.CadastrarVeiculoUseCase;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;

@Service
public class CadastrarVeiculoService implements CadastrarVeiculoUseCase {
    private final VeiculoRepository veiculoRepository;

    public CadastrarVeiculoService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public void cadastrarVeiculo(CadastrarVeiculoCommand command) {
        if (veiculoRepository.buscarPorPlaca(command.placa()).isPresent()) {
            throw new RegraDeNegocioException("Ja existe veiculo cadastrado com a mesma placa.");
        }

        veiculoRepository.salvar(new Veiculo(
                command.placa(),
                command.marca(),
                command.modelo(),
                command.fabricante(),
                command.ano(),
                command.potencia(),
                command.cambio(),
                command.tipo()));
    }
}
