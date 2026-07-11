package br.com.oficina.veiculo.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.ExcluirVeiculoUseCase;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class ExcluirVeiculoService implements ExcluirVeiculoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExcluirVeiculoService.class);

    private final VeiculoRepository veiculoRepository;
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ExcluirVeiculoService(VeiculoRepository veiculoRepository,
            OrdemDeServicoRepository ordemDeServicoRepository) {
        this.veiculoRepository = veiculoRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void excluirVeiculo(ExcluirVeiculoCommand command) {
        log.info("Iniciando exclusao de veiculo. placaInformada={}", command.placa());
        Veiculo veiculo = veiculoRepository.buscarPorPlaca(command.placa())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado para a placa informada."));
        if (ordemDeServicoRepository.existePorVeiculoId(veiculo.getId())) {
            log.warn("Exclusao de veiculo recusada: existem ordens de servico vinculadas. placa={}", veiculo.getPlaca());
            throw new RegraDeNegocioException("Nao e possivel excluir o veiculo: existem ordens de servico vinculadas.");
        }
        veiculoRepository.excluirPorPlaca(command.placa());
        log.info("Veiculo excluido com sucesso. placaInformada={}", command.placa());
    }
}
