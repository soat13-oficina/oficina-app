package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.List;
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
        ordens.put(ordemDeServico.getNumeroOrdemServico(), ordemDeServico);
    }

    @Override
    public Optional<OrdemDeServico> buscarPorNumero(String numeroOrdemServico) {
        return Optional.ofNullable(ordens.get(numeroOrdemServico));
    }

    @Override
    public List<OrdemDeServico> buscarTodas() {
        return ordens.values().stream().toList();
    }

    @Override
    public void excluirPorNumero(String numeroOrdemServico) {
        ordens.remove(numeroOrdemServico);
    }
}
