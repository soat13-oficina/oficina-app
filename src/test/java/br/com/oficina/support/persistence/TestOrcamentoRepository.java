package br.com.oficina.support.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

public class TestOrcamentoRepository implements OrcamentoRepository {
    private final Map<UUID, Orcamento> orcamentos = new ConcurrentHashMap<>();

    @Override
    public Orcamento salvar(Orcamento orcamento) {
        Orcamento orcamentoPersistido = orcamento.getId() == null
                ? Orcamento.reconstituir(
                        UUID.randomUUID(),
                        orcamento.getNumeroOrcamento(),
                        orcamento.getOrdemDeServicoId(),
                        orcamento.getFuncionarioId(),
                        orcamento.getClienteNome(),
                        orcamento.getClienteCpf(),
                        orcamento.getPlacaVeiculo(),
                        orcamento.getMarcaVeiculo(),
                        orcamento.getModeloVeiculo(),
                        orcamento.getDescricaoDiagnostico(),
                        orcamento.getServicosPropostos(),
                        orcamento.getPecasPrevistas(),
                        orcamento.getValorMaoDeObra(),
                        orcamento.getValorPecas(),
                        orcamento.getCriadoEm(),
                        orcamento.getValidade(),
                        orcamento.getObservacoes(),
                        orcamento.getStatus())
                : orcamento;
        if (orcamento.getEnviadoParaAprovacaoEm() != null) {
            orcamentoPersistido.enviarParaAprovacao(orcamento.getEnviadoParaAprovacaoEm());
        }
        orcamentos.put(orcamentoPersistido.getId(), orcamentoPersistido);
        return orcamentoPersistido;
    }

    @Override
    public void atualizar(Orcamento orcamento) {
        orcamentos.put(orcamento.getId(), orcamento);
    }

    @Override
    public void excluirPorNumeroOrcamento(String numeroOrcamento) {
        buscarPorNumeroOrcamento(numeroOrcamento).ifPresent(orcamento -> orcamentos.remove(orcamento.getId()));
    }

    @Override
    public Optional<Orcamento> buscarPorId(UUID id) {
        return Optional.ofNullable(orcamentos.get(id));
    }

    @Override
    public Optional<Orcamento> buscarPorNumeroOrcamento(String numeroOrcamento) {
        return orcamentos.values().stream()
                .filter(orcamento -> orcamento.getNumeroOrcamento().equals(numeroOrcamento))
                .findFirst();
    }

    @Override
    public List<Orcamento> buscarTodos() {
        return List.copyOf(orcamentos.values());
    }
}
