package br.com.oficina.pecainsumo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.RemoverEstoquePecaCommand;
import br.com.oficina.pecainsumo.application.usecase.RemoverEstoquePecaUseCase;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class RemoverEstoquePecaService implements RemoverEstoquePecaUseCase {
    private final PecaInsumoRepository pecaInsumoRepository;

    public RemoverEstoquePecaService(PecaInsumoRepository pecaInsumoRepository) {
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    @Override
    public void removerEstoque(RemoverEstoquePecaCommand command) {
        if (command.quantidade() <= 0) {
            throw new RegraDeNegocioException("A quantidade a ser removida deve ser maior que zero");
        }

        PecaInsumo pecaExistente = pecaInsumoRepository.buscarPorId(command.id())
                .orElseThrow(() -> new RegraDeNegocioException("Peça/Insumo não encontrada com o ID: " + command.id()));

        int novaQuantidade = pecaExistente.getQuantidadeEstoque() - command.quantidade();

        if (novaQuantidade < 0) {
            throw new RegraDeNegocioException(
                    "Estoque insuficiente. Quantidade atual: " + pecaExistente.getQuantidadeEstoque() +
                    ", quantidade solicitada: " + command.quantidade());
        }

        PecaInsumo pecaAtualizada = new PecaInsumo(
                pecaExistente.getId(),
                pecaExistente.getDescricao(),
                pecaExistente.getMarca(),
                pecaExistente.getPreco(),
                novaQuantidade,
                pecaExistente.getQuantidadeReservada(),
                pecaExistente.getCodigoReferencia(),
                pecaExistente.getCategoria());

        pecaInsumoRepository.salvar(pecaAtualizada);
    }
}
