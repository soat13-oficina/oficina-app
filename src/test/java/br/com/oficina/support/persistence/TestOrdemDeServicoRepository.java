package br.com.oficina.support.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                        ordemDeServico.getStatus(),
                        ordemDeServico.getIniciadaEm(),
                        ordemDeServico.getFinalizadaEm(),
                        ordemDeServico.getEntregueEm())
                : ordemDeServico;
        ordens.put(ordemPersistida.getNumeroOrdemServico(), ordemPersistida);
    }

    @Override
    public Optional<OrdemDeServico> buscarPorNumero(String numeroOrdemServico) {
        return Optional.ofNullable(ordens.get(numeroOrdemServico));
    }

    @Override
    public List<OrdemDeServico> buscarPorFiltros(
            String numeroOrdemServico,
            String nomeCliente,
            String placaVeiculo,
            String documentoCliente) {
        return ordens.values().stream()
                .filter(ordem -> numeroOrdemServico == null
                        || ordem.getNumeroOrdemServico().equalsIgnoreCase(numeroOrdemServico))
                .filter(ordem -> nomeCliente == null
                        || ordem.getCliente().getNome().equalsIgnoreCase(nomeCliente))
                .filter(ordem -> placaVeiculo == null
                        || ordem.getVeiculo().getPlaca().equalsIgnoreCase(placaVeiculo))
                .filter(ordem -> documentoCliente == null
                        || (ordem.getCliente().getCpfOuCnpj() != null
                                && ordem.getCliente().getCpfOuCnpj().equals(documentoCliente)))
                .toList();
    }

    @Override
    public List<OrdemDeServico> buscarTodas() {
        return ordens.values().stream().toList();
    }

    @Override
    public void excluirPorNumero(String numeroOrdemServico) {
        ordens.remove(numeroOrdemServico);
    }

    @Override
    public boolean existePorFuncionarioId(UUID funcionarioId) {
        return ordens.values().stream()
                .anyMatch(os -> os.getFuncionario() != null
                        && Objects.equals(funcionarioId, os.getFuncionario().getId()));
    }
}
