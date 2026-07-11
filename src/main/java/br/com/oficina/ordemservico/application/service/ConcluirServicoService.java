package br.com.oficina.ordemservico.application.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ConcluirServicoCommand;
import br.com.oficina.ordemservico.application.usecase.ConcluirServicoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ConcluirServicoService implements ConcluirServicoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConcluirServicoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ConcluirServicoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            ApplicationEventPublisher eventPublisher) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void concluirServico(ConcluirServicoCommand command) {
        log.info("Concluindo servico da ordem de servico. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        SituacaoOrdemDeServico situacaoAnterior = ordemDeServico.getSituacao();
        ordemDeServico.concluirServico();
        ordemDeServicoRepository.salvar(ordemDeServico);
        eventPublisher.publishEvent(new StatusOrdemDeServicoAlterado(
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getCliente().getId(),
                situacaoAnterior,
                ordemDeServico.getSituacao(),
                LocalDateTime.now()));
        log.info("Servico concluido. numeroOrdemServico={}, situacao={}, motivoEncerramento={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getSituacao().getDescricao(),
                ordemDeServico.getMotivoEncerramento());
    }
}
