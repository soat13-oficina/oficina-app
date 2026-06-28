package br.com.oficina.ordemservico.application.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.CadastrarNovoOrcamentoUseCase;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class EnviarDiagnosticoParaOrcamentoService implements EnviarDiagnosticoParaOrcamentoUseCase {
    private static final Logger log = LoggerFactory.getLogger(EnviarDiagnosticoParaOrcamentoService.class);

    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final CadastrarNovoOrcamentoUseCase cadastrarNovoOrcamentoUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public EnviarDiagnosticoParaOrcamentoService(
            OrdemDeServicoRepository ordemDeServicoRepository,
            CadastrarNovoOrcamentoUseCase cadastrarNovoOrcamentoUseCase,
            ApplicationEventPublisher eventPublisher) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.cadastrarNovoOrcamentoUseCase = cadastrarNovoOrcamentoUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void enviarDiagnosticoParaOrcamento(EnviarDiagnosticoParaOrcamentoRequest request) {
        log.info(
                "Iniciando envio de diagnostico para orcamento. numeroOrdemServico={}, quantidadeServicosPropostos={}, quantidadePecasPrevistas={}",
                request.numeroOrdemServico(),
                request.servicosPropostos() == null ? 0 : request.servicosPropostos().size(),
                request.pecasPrevistas() == null ? 0 : request.pecasPrevistas().size());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(request.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));

        SituacaoOrdemDeServico situacaoAnterior = ordemDeServico.getSituacao();
        ordemDeServico.enviarParaAprovacao();

        String numeroOrcamento = "ORC-" + UUID.randomUUID();
        cadastrarNovoOrcamentoUseCase.cadastrarNovoOrcamento(new CadastrarNovoOrcamentoCommand(
                numeroOrcamento,
                ordemDeServico.getCliente().getId().toString(),
                ordemDeServico.getId().toString(),
                ordemDeServico.getFuncionario().getId().toString(),
                ordemDeServico.getVeiculo().getPlaca(),
                ordemDeServico.getVeiculo().getMarca(),
                ordemDeServico.getVeiculo().getModelo(),
                request.descricaoDiagnostico(),
                request.servicosPropostos(),
                request.pecasPrevistas(),
                request.valorMaoDeObra(),
                request.desconto(),
                request.validade(),
                request.observacoes()));

        ordemDeServicoRepository.salvar(ordemDeServico);
        eventPublisher.publishEvent(new StatusOrdemDeServicoAlterado(
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getCliente().getId(),
                situacaoAnterior,
                ordemDeServico.getSituacao(),
                LocalDateTime.now()));
        log.info(
                "Diagnostico enviado para orcamento com sucesso. numeroOrdemServico={}, numeroOrcamento={}, situacao={}",
                ordemDeServico.getNumeroOrdemServico(),
                numeroOrcamento,
                ordemDeServico.getSituacao().getDescricao());
    }
}
