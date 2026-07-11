package br.com.oficina.pecainsumo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.ConsumirPecaCommand;
import br.com.oficina.pecainsumo.application.usecase.ConsumirPecaUseCase;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class ConsumirPecaService implements ConsumirPecaUseCase {
    private final PecaInsumoRepository pecaInsumoRepository;

    public ConsumirPecaService(PecaInsumoRepository pecaInsumoRepository) {
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    @Override
    public void consumirPeca(ConsumirPecaCommand command) {
        if (command.quantidade() <= 0) {
            throw new RegraDeNegocioException("A quantidade a ser consumida deve ser maior que zero");
        }

        PecaInsumo pecaExistente = pecaInsumoRepository.buscarPorId(command.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça/Insumo não encontrada com o ID: " + command.id()));

        if (command.quantidade() > pecaExistente.getQuantidadeReservada()) {
            throw new RegraDeNegocioException(
                    "Quantidade reservada insuficiente para consumo. Reservada atualmente: " + pecaExistente.getQuantidadeReservada() +
                    ", solicitado consumir: " + command.quantidade());
        }

        int novaQuantidadeReservada = pecaExistente.getQuantidadeReservada() - command.quantidade();
        int novaQuantidadeEstoque = pecaExistente.getQuantidadeEstoque() - command.quantidade();

        PecaInsumo pecaAtualizada = new PecaInsumo(
                pecaExistente.getId(),
                pecaExistente.getDescricao(),
                pecaExistente.getMarca(),
                pecaExistente.getPreco(),
                novaQuantidadeEstoque,
                novaQuantidadeReservada,
                pecaExistente.getCodigoReferencia(),
                pecaExistente.getCategoria());

        pecaInsumoRepository.salvar(pecaAtualizada);
    }
}
