package br.com.oficina.pecainsumo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.ReservarPecaCommand;
import br.com.oficina.pecainsumo.application.usecase.ReservarPecaUseCase;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class ReservarPecaService implements ReservarPecaUseCase {
    private final PecaInsumoRepository pecaInsumoRepository;

    public ReservarPecaService(PecaInsumoRepository pecaInsumoRepository) {
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    @Override
    public void reservarPeca(ReservarPecaCommand command) {
        if (command.quantidade() <= 0) {
            throw new RegraDeNegocioException("A quantidade a ser reservada deve ser maior que zero");
        }

        PecaInsumo pecaExistente = pecaInsumoRepository.buscarPorId(command.id())
                .orElseThrow(() -> new RegraDeNegocioException("Peça/Insumo não encontrada com o ID: " + command.id()));

        int novaQuantidadeReservada = pecaExistente.getQuantidadeReservada() + command.quantidade();

        if (novaQuantidadeReservada > pecaExistente.getQuantidadeEstoque()) {
            throw new RegraDeNegocioException(
                    "Quantidade insuficiente para reserva. Disponível: " + pecaExistente.getQuantidadeDisponivel() +
                    ", solicitado: " + command.quantidade());
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
