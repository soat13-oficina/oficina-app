package br.com.oficina.ordemservico.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;
import br.com.oficina.ordemservico.application.port.CatalogoPecas;
import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand.PecaDiagnosticoInput;
import br.com.oficina.ordemservico.application.usecase.ConcluirDiagnosticoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.PecaPrevistaOrdem;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ConcluirDiagnosticoService implements ConcluirDiagnosticoUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConcluirDiagnosticoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CatalogoPecas catalogoPecas;

    @Autowired
    public ConcluirDiagnosticoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            ApplicationEventPublisher eventPublisher,
            CatalogoPecas catalogoPecas) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.eventPublisher = eventPublisher;
        this.catalogoPecas = catalogoPecas;
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
        LocalDateTime anteriorDesde = ordemDeServico.getSituacaoAlteradaEm();

        if (command.temDadosDeDiagnostico()) {
            ordemDeServico.concluirDiagnostico(command.descricaoServico(), resolverPecas(command.pecas()));
        } else {
            ordemDeServico.concluirDiagnostico();
        }

        ordemDeServicoRepository.salvar(ordemDeServico);
        // DIAGNOSTICO_EM_ANDAMENTO e DIAGNOSTICO_CONCLUIDO mapeiam para a mesma SituacaoOrdemDeServico;
        // sem essa checagem o cliente recebe duas notificacoes identicas de "Diagnostico".
        if (situacaoAnterior != ordemDeServico.getSituacao()) {
            eventPublisher.publishEvent(StatusOrdemDeServicoAlterado.de(ordemDeServico, situacaoAnterior, anteriorDesde));
        }
        log.info("Diagnostico concluido com sucesso. numeroOrdemServico={}, statusAtual={}",
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getStatus());
    }

    private List<PecaPrevistaOrdem> resolverPecas(List<PecaDiagnosticoInput> pecas) {
        List<PecaPrevistaOrdem> resultado = new ArrayList<>();
        if (!pecas.isEmpty()) {
            Set<String> inexistentes = catalogoPecas == null
                    ? Set.copyOf(pecas.stream().map(PecaDiagnosticoInput::pecaInsumoId).toList())
                    : catalogoPecas.inexistentes(pecas.stream().map(PecaDiagnosticoInput::pecaInsumoId).toList());
            if (!inexistentes.isEmpty()) {
                throw new RecursoNaoEncontradoException("Peca/Insumo nao encontrada com o ID: " + String.join(", ", inexistentes));
            }
        }
        for (PecaDiagnosticoInput peca : pecas) {
            resultado.add(new PecaPrevistaOrdem(peca.pecaInsumoId(), peca.quantidade()));
        }
        return resultado;
    }
}
