package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ConcluirServicoCommand;
import br.com.oficina.ordemservico.application.usecase.ConcluirServicoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ConcluirServicoService implements ConcluirServicoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConcluirServicoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ConcluirServicoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void concluirServico(ConcluirServicoCommand command) {
        log.info("Concluindo servico da ordem de servico. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        ordemDeServico.concluirServico();
        ordemDeServicoRepository.salvar(ordemDeServico);
        log.info("Servico concluido. numeroOrdemServico={}, situacao={}, motivoEncerramento={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getSituacao().getDescricao(),
                ordemDeServico.getMotivoEncerramento());
    }
}
