package br.com.oficina.orcamento.application.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.AprovarOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.AprovarOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.PecaOrcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;
import br.com.oficina.pecainsumo.application.command.LiberarReservaPecaCommand;
import br.com.oficina.pecainsumo.application.command.ReservarPecaCommand;
import br.com.oficina.pecainsumo.application.usecase.LiberarReservaPecaUseCase;
import br.com.oficina.pecainsumo.application.usecase.ReservarPecaUseCase;

@Service
public class AprovarOrcamentoService implements AprovarOrcamentoUseCase {
    private static final Logger log = LoggerFactory.getLogger(AprovarOrcamentoService.class);

    private final OrcamentoRepository orcamentoRepository;
    private final ReservarPecaUseCase reservarPecaUseCase;
    private final LiberarReservaPecaUseCase liberarReservaPecaUseCase;

    @Autowired
    public AprovarOrcamentoService(
            OrcamentoRepository orcamentoRepository,
            ReservarPecaUseCase reservarPecaUseCase,
            LiberarReservaPecaUseCase liberarReservaPecaUseCase) {
        this.orcamentoRepository = orcamentoRepository;
        this.reservarPecaUseCase = reservarPecaUseCase;
        this.liberarReservaPecaUseCase = liberarReservaPecaUseCase;
    }

    public AprovarOrcamentoService(
            OrcamentoRepository orcamentoRepository,
            ReservarPecaUseCase reservarPecaUseCase) {
        this(orcamentoRepository, reservarPecaUseCase, command -> {
        });
    }

    @Override
    public Orcamento aprovarOrcamento(AprovarOrcamentoCommand command) {
        log.info("Iniciando aprovacao de orcamento. numeroOrcamento={}", command.numeroOrcamento());
        Orcamento orcamento = orcamentoRepository.buscarPorNumeroOrcamento(command.numeroOrcamento())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para o numero informado."));

        if (orcamento.getStatus() != StatusOrcamento.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Orcamento so pode ser aprovado quando estiver aguardando aprovacao.");
        }

        List<String> pecasSemEstoque = new ArrayList<>();
        List<PecaOrcamento> pecasReservadas = new ArrayList<>();

        for (PecaOrcamento peca : orcamento.getPecasOrcamento()) {
            try {
                reservarPecaUseCase.reservarPeca(
                        new ReservarPecaCommand(peca.getPecaInsumoId(), peca.getQuantidade()));
                pecasReservadas.add(peca);
            } catch (RegraDeNegocioException e) {
                pecasSemEstoque.add(peca.getDescricao() + " (id: " + peca.getPecaInsumoId() + ", qtd: " + peca.getQuantidade() + ")");
            }
        }

        if (!pecasSemEstoque.isEmpty()) {
            for (PecaOrcamento pecaReservada : pecasReservadas) {
                try {
                    liberarReservaPecaUseCase.liberarReserva(new LiberarReservaPecaCommand(
                            pecaReservada.getPecaInsumoId(),
                            pecaReservada.getQuantidade()));
                } catch (Exception excecao) {
                    log.warn("Falha ao desfazer reserva da peca {}. Pode ser necessario ajuste manual.",
                            pecaReservada.getPecaInsumoId(), excecao);
                }
            }
            throw new RegraDeNegocioException(
                    "Estoque insuficiente para as seguintes pecas: " + String.join(", ", pecasSemEstoque)
                    + ". O orcamento permanece aguardando aprovacao (AGUARDANDO_APROVACAO).");
        }

        orcamento.aprovar();
        orcamentoRepository.atualizar(orcamento);
        log.info("Orcamento aprovado com sucesso. numeroOrcamento={}, pecasReservadas={}",
                command.numeroOrcamento(), pecasReservadas.size());
        return orcamento;
    }
}
