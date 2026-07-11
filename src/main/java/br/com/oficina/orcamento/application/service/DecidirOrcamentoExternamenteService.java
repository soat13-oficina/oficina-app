package br.com.oficina.orcamento.application.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.ConflitoDeRecursoException;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.AprovarOrcamentoCommand;
import br.com.oficina.orcamento.application.command.DecidirOrcamentoExternamenteCommand;
import br.com.oficina.orcamento.application.command.RejeitarOrcamentoCommand;
import br.com.oficina.orcamento.application.result.DecisaoOrcamentoResult;
import br.com.oficina.orcamento.application.usecase.AprovarOrcamentoUseCase;
import br.com.oficina.orcamento.application.usecase.DecidirOrcamentoExternamenteUseCase;
import br.com.oficina.orcamento.application.usecase.RejeitarOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.DecisaoOrcamento;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class DecidirOrcamentoExternamenteService implements DecidirOrcamentoExternamenteUseCase {
    private static final Logger log = LoggerFactory.getLogger(DecidirOrcamentoExternamenteService.class);

    private final OrcamentoRepository orcamentoRepository;
    private final OrdemDeServicoRepository ordemDeServicoRepository;
    private final AprovarOrcamentoUseCase aprovarOrcamentoUseCase;
    private final RejeitarOrcamentoUseCase rejeitarOrcamentoUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public DecidirOrcamentoExternamenteService(
            OrcamentoRepository orcamentoRepository,
            OrdemDeServicoRepository ordemDeServicoRepository,
            AprovarOrcamentoUseCase aprovarOrcamentoUseCase,
            RejeitarOrcamentoUseCase rejeitarOrcamentoUseCase,
            ApplicationEventPublisher eventPublisher) {
        this.orcamentoRepository = orcamentoRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.aprovarOrcamentoUseCase = aprovarOrcamentoUseCase;
        this.rejeitarOrcamentoUseCase = rejeitarOrcamentoUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public DecisaoOrcamentoResult decidir(DecidirOrcamentoExternamenteCommand command) {
        if (command.decisao() == null) {
            throw new RegraDeNegocioException("Decisao do orcamento e obrigatoria.");
        }

        log.info("Recebida decisao externa de orcamento. numeroOrcamento={}, decisao={}",
                command.numeroOrcamento(),
                command.decisao());
        Orcamento orcamento = orcamentoRepository.buscarPorNumeroOrcamento(command.numeroOrcamento())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para o numero informado."));
        OrdemDeServico ordem = ordemDeServicoRepository.buscarPorId(orcamento.getOrdemDeServicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico vinculada ao orcamento nao encontrada."));

        if (orcamento.getStatus() == StatusOrcamento.APROVADO || orcamento.getStatus() == StatusOrcamento.REJEITADO) {
            return tratarDecisaoJaRegistrada(command, orcamento, ordem);
        }

        if (orcamento.getStatus() != StatusOrcamento.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Orcamento so pode receber decisao quando estiver aguardando aprovacao.");
        }
        if (ordem.getStatus() != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Ordem de servico so pode receber decisao quando estiver aguardando aprovacao.");
        }

        SituacaoOrdemDeServico situacaoAnterior = ordem.getSituacao();
        if (command.decisao() == DecisaoOrcamento.APROVADO) {
            aprovarOrcamentoUseCase.aprovarOrcamento(new AprovarOrcamentoCommand(command.numeroOrcamento()));
            ordem.iniciarExecucao();
        } else {
            rejeitarOrcamentoUseCase.rejeitarOrcamento(new RejeitarOrcamentoCommand(command.numeroOrcamento()));
            ordem.recusarOrcamento();
        }

        ordemDeServicoRepository.salvar(ordem);
        eventPublisher.publishEvent(new StatusOrdemDeServicoAlterado(
                ordem.getNumeroOrdemServico(),
                ordem.getCliente().getId(),
                situacaoAnterior,
                ordem.getSituacao(),
                LocalDateTime.now()));
        return resposta(command.numeroOrcamento(), ordem);
    }

    private DecisaoOrcamentoResult tratarDecisaoJaRegistrada(
            DecidirOrcamentoExternamenteCommand command,
            Orcamento orcamento,
            OrdemDeServico ordem) {
        boolean mesmaDecisao = (orcamento.getStatus() == StatusOrcamento.APROVADO
                && command.decisao() == DecisaoOrcamento.APROVADO)
                || (orcamento.getStatus() == StatusOrcamento.REJEITADO
                && command.decisao() == DecisaoOrcamento.REJEITADO);
        if (!mesmaDecisao) {
            throw new ConflitoDeRecursoException("Decisao divergente da decisao ja registrada para o orcamento.");
        }
        return resposta(command.numeroOrcamento(), ordem);
    }

    private static DecisaoOrcamentoResult resposta(String numeroOrcamento, OrdemDeServico ordem) {
        return new DecisaoOrcamentoResult(
                numeroOrcamento,
                ordem.getNumeroOrdemServico(),
                ordem.getSituacao().getDescricao());
    }
}
