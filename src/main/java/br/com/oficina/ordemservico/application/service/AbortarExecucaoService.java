package br.com.oficina.ordemservico.application.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.AbortarExecucaoCommand;
import br.com.oficina.ordemservico.application.usecase.AbortarExecucaoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.saga.application.PassosDaSaga;

/**
 * Mecanico aborta a execucao (peca errada, defeito que impede o servico...). A OS e cancelada e a
 * Saga compensa: estorna o pagamento e libera as pecas reservadas (ADR 0004, passo 6 -
 * {@code ExecucaoFalhou}). Na Execution Service vira {@code POST /execucoes/{ordemServicoId}/abortar}.
 */
@Service
public class AbortarExecucaoService implements AbortarExecucaoUseCase {
    private static final Logger log = LoggerFactory.getLogger(AbortarExecucaoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PassosDaSaga passosDaSaga;

    public AbortarExecucaoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            ApplicationEventPublisher eventPublisher,
            PassosDaSaga passosDaSaga) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.eventPublisher = eventPublisher;
        this.passosDaSaga = passosDaSaga;
    }

    @Override
    @Transactional
    public void abortarExecucao(AbortarExecucaoCommand command) {
        if (command.motivo() == null || command.motivo().isBlank()) {
            throw new RegraDeNegocioException("Motivo do aborto da execucao e obrigatorio.");
        }
        log.info("Abortando execucao da ordem de servico. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        SituacaoOrdemDeServico situacaoAnterior = ordemDeServico.getSituacao();
        LocalDateTime anteriorDesde = ordemDeServico.getSituacaoAlteradaEm();
        ordemDeServico.abortarExecucao();
        ordemDeServicoRepository.salvar(ordemDeServico);
        passosDaSaga.execucaoFalhou(ordemDeServico.getId(), command.motivo());
        eventPublisher.publishEvent(StatusOrdemDeServicoAlterado.de(ordemDeServico, situacaoAnterior, anteriorDesde));
        log.info("Execucao abortada; Saga compensando. numeroOrdemServico={}, motivo={}",
                ordemDeServico.getNumeroOrdemServico(), command.motivo());
    }
}
