package br.com.oficina.ordemservico.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;
import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand.PecaDiagnosticoInput;
import br.com.oficina.ordemservico.application.usecase.ConcluirDiagnosticoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.PecaPrevistaOrdem;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class ConcluirDiagnosticoService implements ConcluirDiagnosticoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConcluirDiagnosticoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PecaInsumoRepository pecaInsumoRepository;

    @Autowired
    public ConcluirDiagnosticoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            ApplicationEventPublisher eventPublisher,
            PecaInsumoRepository pecaInsumoRepository) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.eventPublisher = eventPublisher;
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    public ConcluirDiagnosticoService(OrdemDeServicoRepository ordemDeServicoRepository) {
        this(ordemDeServicoRepository, event -> {
        }, null);
    }

    @Override
    public void concluirDiagnostico(ConcluirDiagnosticoCommand command) {
        log.info("Concluindo diagnostico de ordem de servico. numeroOrdemServico={}", command.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        SituacaoOrdemDeServico situacaoAnterior = ordemDeServico.getSituacao();

        if (command.temDadosDeDiagnostico()) {
            ordemDeServico.concluirDiagnostico(command.descricaoServico(), resolverPecas(command.pecas()));
        } else {
            ordemDeServico.concluirDiagnostico();
        }

        ordemDeServicoRepository.salvar(ordemDeServico);
        eventPublisher.publishEvent(new StatusOrdemDeServicoAlterado(
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getCliente().getId(),
                situacaoAnterior,
                ordemDeServico.getSituacao(),
                LocalDateTime.now()));
        log.info("Diagnostico concluido com sucesso. numeroOrdemServico={}, statusAtual={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getStatus());
    }

    private List<PecaPrevistaOrdem> resolverPecas(List<PecaDiagnosticoInput> pecas) {
        List<PecaPrevistaOrdem> resultado = new ArrayList<>();
        for (PecaDiagnosticoInput peca : pecas) {
            if (pecaInsumoRepository == null || pecaInsumoRepository.buscarPorId(peca.pecaInsumoId()).isEmpty()) {
                throw new RecursoNaoEncontradoException("Peca/Insumo nao encontrada com o ID: " + peca.pecaInsumoId());
            }
            resultado.add(new PecaPrevistaOrdem(peca.pecaInsumoId(), peca.quantidade()));
        }
        return resultado;
    }
}
