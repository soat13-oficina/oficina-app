package br.com.oficina.support.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
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
                        ordemDeServico.getEntregueEm(),
                        ordemDeServico.getMotivoEncerramento(),
                        ordemDeServico.getServicos(),
                        ordemDeServico.getPecasPrevistas())
                : ordemDeServico;
        ordens.put(ordemPersistida.getNumeroOrdemServico(), ordemPersistida);
    }

    @Override
    public Optional<OrdemDeServico> buscarPorId(UUID ordemDeServicoId) {
        return ordens.values().stream()
                .filter(ordem -> Objects.equals(ordem.getId(), ordemDeServicoId))
                .findFirst();
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
    public List<OrdemDeServico> buscarAtivasPriorizadasPorFiltros(
            String numeroOrdemServico,
            String nomeCliente,
            String placaVeiculo,
            String documentoCliente) {
        return buscarPorFiltros(numeroOrdemServico, nomeCliente, placaVeiculo, documentoCliente).stream()
                .filter(ordem -> ordem.getStatus() != StatusOrdemDeServico.OS_FINALIZADA
                        && ordem.getStatus() != StatusOrdemDeServico.ENTREGUE)
                .sorted(Comparator
                        .comparingInt((OrdemDeServico ordem) -> prioridade(ordem.getStatus()))
                        .thenComparing(ordem -> ordem.getIniciadaEm(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OrdemDeServico::getNumeroOrdemServico))
                .toList();
    }

    @Override
    public List<OrdemDeServico> buscarTodas() {
        return ordens.values().stream().toList();
    }

    @Override
    public List<OrdemDeServico> buscarOrdensComExecucaoFinalizada() {
        return ordens.values().stream()
                .filter(ordem -> ordem.getIniciadaEm() != null && ordem.getFinalizadaEm() != null)
                .toList();
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

    @Override
    public boolean existePorVeiculoId(UUID veiculoId) {
        return ordens.values().stream()
                .anyMatch(os -> Objects.equals(veiculoId, os.getVeiculoId()));
    }

    private static int prioridade(StatusOrdemDeServico status) {
        return switch (status) {
            case SERVICO_EM_ANDAMENTO -> 1;
            case AGUARDANDO_APROVACAO -> 2;
            case DIAGNOSTICO_EM_ANDAMENTO, DIAGNOSTICO_CONCLUIDO -> 3;
            case OS_ABERTA -> 4;
            default -> 5;
        };
    }
}
