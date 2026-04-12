package br.com.oficina.pecainsumo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.pecainsumo.application.command.CadastrarPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.usecase.CadastrarPecaInsumoUseCase;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class CadastrarPecaInsumoService implements CadastrarPecaInsumoUseCase {
    private final PecaInsumoRepository pecaInsumoRepository;

    public CadastrarPecaInsumoService(PecaInsumoRepository pecaInsumoRepository) {
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    @Override
    public void cadastrarPecaInsumo(CadastrarPecaInsumoCommand command) {
        pecaInsumoRepository.salvar(new PecaInsumo(
                command.descricao(),
                command.marca(),
                command.preco(),
                command.quantidadeEstoque(),
                command.codigoReferencia(),
                command.categoria()));
    }
}
