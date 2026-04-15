package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ExcluirOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.usecase.ExcluirOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ExcluirOrdemDeServicoService implements ExcluirOrdemDeServicoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExcluirOrdemDeServicoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ExcluirOrdemDeServicoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void excluirOrdemDeServico(ExcluirOrdemDeServicoCommand command) {
        log.info("Iniciando exclusao de ordem de servico. numeroOrdemServico={}", command.numeroOrdemServico());
        ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        ordemDeServicoRepository.excluirPorNumero(command.numeroOrdemServico());
        log.info("Ordem de servico excluida com sucesso. numeroOrdemServico={}", command.numeroOrdemServico());
    }
}
