package br.com.oficina.orcamento.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.oficina.orcamento.domain.model.Orcamento;

public interface OrcamentoRepository {
    Orcamento salvar(Orcamento orcamento);

    void atualizar(Orcamento orcamento);

    void excluirPorNumeroOrcamento(String numeroOrcamento);

    Optional<Orcamento> buscarPorId(UUID id);

    Optional<Orcamento> buscarPorNumeroOrcamento(String numeroOrcamento);

    Optional<Orcamento> buscarPorOrdemDeServicoId(String ordemDeServicoId);

    List<Orcamento> buscarTodos();
}
