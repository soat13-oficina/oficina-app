package br.com.oficina.pecainsumo.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.pecainsumo.application.command.ExcluirPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.usecase.ExcluirPecaInsumoUseCase;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Service
public class ExcluirPecaInsumoService implements ExcluirPecaInsumoUseCase {
    private final PecaInsumoRepository pecaInsumoRepository;

    public ExcluirPecaInsumoService(PecaInsumoRepository pecaInsumoRepository) {
        this.pecaInsumoRepository = pecaInsumoRepository;
    }

    @Override
    public void excluirPecaInsumo(ExcluirPecaInsumoCommand command) {
        pecaInsumoRepository.excluirPorId(command.id());
    }
}
