package br.com.oficina.veiculo.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.application.command.AlterarVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.AlterarVeiculoUseCase;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class AlterarVeiculoService implements AlterarVeiculoUseCase {
    private static final Logger log = LoggerFactory.getLogger(AlterarVeiculoService.class);

    private final VeiculoRepository veiculoRepository;

    public AlterarVeiculoService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public void alterarVeiculo(AlterarVeiculoCommand command) {
        log.info("Iniciando alteracao de veiculo. placaInformada={}, marca={}, fabricante={}",
                command.placa(),
                command.marca(),
                command.fabricante());
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
        log.info("Veiculo alterado com sucesso. veiculoId={}, placa={}", veiculo.getId(), veiculo.getPlaca());
    }
}
