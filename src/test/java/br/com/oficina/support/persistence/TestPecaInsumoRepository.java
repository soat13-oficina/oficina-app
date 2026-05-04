package br.com.oficina.support.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

public class TestPecaInsumoRepository implements PecaInsumoRepository {
    private final Map<String, PecaInsumo> pecas = new ConcurrentHashMap<>();

    @Override
    public void salvar(PecaInsumo pecaInsumo) {
        pecas.put(pecaInsumo.getId(), pecaInsumo);
    }

    @Override
    public Optional<PecaInsumo> buscarPorId(String id) {
        return Optional.ofNullable(pecas.get(id));
    }

    @Override
    public List<PecaInsumo> buscarTodos() {
        return List.copyOf(pecas.values());
    }

    @Override
    public void excluirPorId(String id) {
        pecas.remove(id);
    }
}
