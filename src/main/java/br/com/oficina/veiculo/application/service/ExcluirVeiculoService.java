package br.com.oficina.veiculo.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.ExcluirVeiculoUseCase;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class ExcluirVeiculoService implements ExcluirVeiculoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExcluirVeiculoService.class);

    private final VeiculoRepository veiculoRepository;

    public ExcluirVeiculoService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public void excluirVeiculo(ExcluirVeiculoCommand command) {
        log.info("Iniciando exclusao de veiculo. placaInformada={}", command.placa());
        veiculoRepository.buscarPorPlaca(command.placa())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado para a placa informada."));
        veiculoRepository.excluirPorPlaca(command.placa());
        log.info("Veiculo excluido com sucesso. placaInformada={}", command.placa());
    }
}
