package br.com.oficina.pecainsumo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.pecainsumo.application.command.AlterarPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.usecase.AlterarPecaInsumoUseCase;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class AlterarPecaInsumoService implements AlterarPecaInsumoUseCase {
    private final PecaInsumoRepository pecaInsumoRepository;

    public AlterarPecaInsumoService(PecaInsumoRepository pecaInsumoRepository) {
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    @Override
    public void alterarPecaInsumo(AlterarPecaInsumoCommand command) {
        pecaInsumoRepository.salvar(new PecaInsumo(
                command.id(),
                command.descricao(),
                command.marca(),
                command.preco(),
                command.quantidadeEstoque(),
                command.quantidadeReservada(),
                command.codigoReferencia(),
                command.categoria()));
    }
}
