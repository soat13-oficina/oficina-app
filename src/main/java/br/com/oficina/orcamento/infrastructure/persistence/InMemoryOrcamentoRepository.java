package br.com.oficina.orcamento.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Repository
public class InMemoryOrcamentoRepository implements OrcamentoRepository {
    private final Map<String, Orcamento> orcamentos = new ConcurrentHashMap<>();

    @Override
    public void salvar(Orcamento orcamento) {
        orcamentos.put(orcamento.getId(), orcamento);
    }

    @Override
    public void atualizar(Orcamento orcamento) {
        orcamentos.put(orcamento.getId(), orcamento);
    }

    @Override
    public void excluirPorId(String orcamentoId) {
        orcamentos.remove(orcamentoId);
    }

    @Override
    public Optional<Orcamento> buscarPorId(String orcamentoId) {
        return Optional.ofNullable(orcamentos.get(orcamentoId));
    }

    @Override
    public List<Orcamento> buscarTodos() {
        return List.copyOf(orcamentos.values());
    }
}
