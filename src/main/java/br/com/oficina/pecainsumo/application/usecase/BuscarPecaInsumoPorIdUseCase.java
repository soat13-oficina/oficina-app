package br.com.oficina.pecainsumo.application.usecase;

import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuscarPecaInsumoPorIdUseCase {

    private final PecaInsumoRepository repository;

    public Optional<PecaInsumo> buscar(String id) {
        return repository.buscarPorId(id);
    }
}
