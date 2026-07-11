package br.com.oficina.pecainsumo.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.ExcluirPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.usecase.ExcluirPecaInsumoUseCase;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class ExcluirPecaInsumoService implements ExcluirPecaInsumoUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcluirPecaInsumoService.class);

    private final PecaInsumoRepository pecaInsumoRepository;

    public ExcluirPecaInsumoService(PecaInsumoRepository pecaInsumoRepository) {
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    @Override
    public void excluirPecaInsumo(ExcluirPecaInsumoCommand command) {
        PecaInsumo peca = pecaInsumoRepository.buscarPorId(command.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Peça/Insumo não encontrada com o ID: " + command.id()));

        if (peca.getQuantidadeReservada() > 0) {
            LOGGER.warn(
                    "Exclusao de peca/insumo bloqueada por reserva ativa: id={}, quantidadeReservada={}",
                    command.id(),
                    peca.getQuantidadeReservada());
            throw new RegraDeNegocioException("Não é possível excluir peça/insumo com reserva ativa.");
        }

        pecaInsumoRepository.excluirPorId(command.id());
        LOGGER.info("Peca/insumo excluida com sucesso: id={}", command.id());
    }
}
