package br.com.oficina.ordemservico.application.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.IniciarDiagnosticoCommand;
import br.com.oficina.ordemservico.application.usecase.IniciarDiagnosticoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class IniciarDiagnosticoService implements IniciarDiagnosticoUseCase {
    private static final Logger log = LoggerFactory.getLogger(IniciarDiagnosticoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public IniciarDiagnosticoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            ApplicationEventPublisher eventPublisher) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.eventPublisher = eventPublisher;
    }

    public IniciarDiagnosticoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this(ordemDeServicoRepository, event -> {
        });
    }

    @Override
    public void iniciarDiagnostico(IniciarDiagnosticoCommand command) {
        log.info("Iniciando fluxo de diagnostico. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        SituacaoOrdemDeServico situacaoAnterior = ordemDeServico.getSituacao();
        ordemDeServico.iniciarDiagnostico();
        ordemDeServicoRepository.salvar(ordemDeServico);
        eventPublisher.publishEvent(new StatusOrdemDeServicoAlterado(
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getCliente().getId(),
                situacaoAnterior,
                ordemDeServico.getSituacao(),
                LocalDateTime.now()));
        log.info("Diagnostico iniciado com sucesso. numeroOrdemServico={}, statusAtual={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getStatus());
    }
}
