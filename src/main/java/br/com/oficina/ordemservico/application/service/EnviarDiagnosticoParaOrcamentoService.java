package br.com.oficina.ordemservico.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;
import br.com.oficina.orcamento.application.command.PecaOrcamentoInput;
import br.com.oficina.orcamento.application.usecase.CadastrarNovoOrcamentoUseCase;
import br.com.oficina.ordemservico.application.usecase.EnviarDiagnosticoParaOrcamentoUseCase;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.PecaPrevistaOrdem;
import br.com.oficina.ordemservico.domain.model.ServicoOrdem;
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
        log.info("Iniciando envio de diagnostico para orcamento. numeroOrdemServico={}", request.numeroOrdemServico());
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(request.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));

        // O diagnostico deve ter sido fechado com peças/descrição (fonte do orçamento).
        if (ordemDeServico.getServicos().isEmpty() && ordemDeServico.getPecasPrevistas().isEmpty()) {
            throw new RegraDeNegocioException(
                    "Diagnostico nao fechado: registre as pecas e a descricao do servico no fechamento do diagnostico.");
        }

        SituacaoOrdemDeServico situacaoAnterior = ordemDeServico.getSituacao();
        ordemDeServico.enviarParaAprovacao();

        // Deriva descrição/serviços e peças do diagnóstico (na OS); financeiro vem do request.
        List<String> servicosPropostos = ordemDeServico.getServicos().stream()
                .map(ServicoOrdem::getDescricao)
                .toList();
        String descricaoServico = servicosPropostos.isEmpty() ? null : String.join("; ", servicosPropostos);
        List<PecaOrcamentoInput> pecas = ordemDeServico.getPecasPrevistas().stream()
                .map(p -> new PecaOrcamentoInput(p.getPecaInsumoId(), p.getQuantidade()))
                .toList();

        String numeroOrcamento = "ORC-" + UUID.randomUUID();
        cadastrarNovoOrcamentoUseCase.cadastrarNovoOrcamento(new CadastrarNovoOrcamentoCommand(
                numeroOrcamento,
                ordemDeServico.getCliente().getId().toString(),
                ordemDeServico.getId().toString(),
                ordemDeServico.getFuncionario().getId().toString(),
                ordemDeServico.getVeiculo().getPlaca(),
                ordemDeServico.getVeiculo().getMarca(),
                ordemDeServico.getVeiculo().getModelo(),
                descricaoServico,
                servicosPropostos,
                pecas,
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
        log.info("Diagnostico enviado para orcamento com sucesso. numeroOrdemServico={}, numeroOrcamento={}, situacao={}",
                ordemDeServico.getNumeroOrdemServico(),
                numeroOrcamento,
                ordemDeServico.getSituacao().getDescricao());
    }
}
