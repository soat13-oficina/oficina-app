package br.com.oficina.pecainsumo.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Repository
public class InMemoryPecaInsumoRepository implements PecaInsumoRepository {
    private final Map<String, PecaInsumo> pecasInsumos = new ConcurrentHashMap<>();

    @Override
    public void salvar(PecaInsumo pecaInsumo) {
        pecasInsumos.put(pecaInsumo.getId(), pecaInsumo);
    }

    @Override
    public Optional<PecaInsumo> buscarPorId(String id) {
        return Optional.ofNullable(pecasInsumos.get(id));
    }

    @Override
    public List<PecaInsumo> buscarTodos() {
        return List.copyOf(pecasInsumos.values());
    }

    @Override
    public void excluirPorId(String id) {
        pecasInsumos.remove(id);
    }
}
