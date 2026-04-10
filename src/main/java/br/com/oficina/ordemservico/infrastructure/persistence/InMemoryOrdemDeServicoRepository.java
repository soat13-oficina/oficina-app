package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Repository
public class InMemoryOrdemDeServicoRepository implements OrdemDeServicoRepository {
    private final Map<String, OrdemDeServico> ordens = new ConcurrentHashMap<>();

    @Override
    public void salvar(OrdemDeServico ordemDeServico) {
        ordens.put(ordemDeServico.getId(), ordemDeServico);
    }

    @Override
    public Optional<OrdemDeServico> buscarPorId(String ordemDeServicoId) {
        return Optional.ofNullable(ordens.get(ordemDeServicoId));
    }
}
