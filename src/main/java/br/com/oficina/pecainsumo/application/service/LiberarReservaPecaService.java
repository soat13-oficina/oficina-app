package br.com.oficina.pecainsumo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.LiberarReservaPecaCommand;
import br.com.oficina.pecainsumo.application.usecase.LiberarReservaPecaUseCase;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class LiberarReservaPecaService implements LiberarReservaPecaUseCase {
    private final PecaInsumoRepository pecaInsumoRepository;

    public LiberarReservaPecaService(PecaInsumoRepository pecaInsumoRepository) {
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    @Override
    public void liberarReserva(LiberarReservaPecaCommand command) {
        if (command.quantidade() <= 0) {
            throw new RegraDeNegocioException("A quantidade a ser liberada deve ser maior que zero");
        }

        PecaInsumo pecaExistente = pecaInsumoRepository.buscarPorId(command.id())
                .orElseThrow(() -> new RegraDeNegocioException("Peça/Insumo não encontrada com o ID: " + command.id()));

        int novaQuantidadeReservada = pecaExistente.getQuantidadeReservada() - command.quantidade();

        if (novaQuantidadeReservada < 0) {
            throw new RegraDeNegocioException(
                    "Quantidade reservada insuficiente. Reservada atualmente: " + pecaExistente.getQuantidadeReservada() +
                    ", solicitado liberar: " + command.quantidade());
        }

        PecaInsumo pecaAtualizada = new PecaInsumo(
                pecaExistente.getId(),
                pecaExistente.getDescricao(),
                pecaExistente.getMarca(),
                pecaExistente.getPreco(),
                pecaExistente.getQuantidadeEstoque(),
                novaQuantidadeReservada,
                pecaExistente.getCodigoReferencia(),
                pecaExistente.getCategoria());

        pecaInsumoRepository.salvar(pecaAtualizada);
    }
}
