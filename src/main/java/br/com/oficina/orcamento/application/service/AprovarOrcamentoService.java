package br.com.oficina.orcamento.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.AprovarOrcamentoCommand;
import br.com.oficina.orcamento.application.saga.EventosOrcamento;
import br.com.oficina.orcamento.application.usecase.AprovarOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class AprovarOrcamentoService implements AprovarOrcamentoUseCase {
    private static final Logger log = LoggerFactory.getLogger(AprovarOrcamentoService.class);

    private final OrcamentoRepository orcamentoRepository;
    private final EventosOrcamento eventos;

    public AprovarOrcamentoService(OrcamentoRepository orcamentoRepository, EventosOrcamento eventos) {
        this.orcamentoRepository = orcamentoRepository;
        this.eventos = eventos;
    }

    /**
     * Aprova e publica {@code OrcamentoAprovado} na mesma transacao. Fase 4: a reserva das pecas nao
     * acontece mais aqui - e o passo {@code ReservarPecas} da Saga; faltando estoque, a Saga reverte
     * a aprovacao ({@code ReverterAprovacao}) e o orcamento volta a aguardar o cliente (ADR 0004).
     */
    @Override
    @Transactional
    public Orcamento aprovarOrcamento(AprovarOrcamentoCommand command) {
        log.info("Iniciando aprovacao de orcamento. numeroOrcamento={}", command.numeroOrcamento());
        Orcamento orcamento = orcamentoRepository.buscarPorNumeroOrcamento(command.numeroOrcamento())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para o numero informado."));

        if (orcamento.getStatus() != StatusOrcamento.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Orcamento so pode ser aprovado quando estiver aguardando aprovacao.");
        }

        orcamento.aprovar();
        orcamentoRepository.atualizar(orcamento);
        eventos.orcamentoAprovado(orcamento, command.canal());
        log.info("Orcamento aprovado; reserva das pecas segue pela Saga. numeroOrcamento={}", command.numeroOrcamento());
        return orcamento;
    }
}
