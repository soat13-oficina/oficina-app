package br.com.oficina.support.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

public class TestOrdemDeServicoRepository implements OrdemDeServicoRepository {
    private final Map<String, OrdemDeServico> ordens = new ConcurrentHashMap<>();

    @Override
    public void salvar(OrdemDeServico ordemDeServico) {
        OrdemDeServico ordemPersistida = ordemDeServico.getId() == null
                ? OrdemDeServico.reconstituir(
                        UUID.randomUUID(),
                        ordemDeServico.getNumeroOrdemServico(),
                        ordemDeServico.getFuncionario(),
                        ordemDeServico.getCliente(),
                        ordemDeServico.getVeiculo(),
                        ordemDeServico.getStatus())
                : ordemDeServico;
        ordens.put(ordemPersistida.getNumeroOrdemServico(), ordemPersistida);
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
