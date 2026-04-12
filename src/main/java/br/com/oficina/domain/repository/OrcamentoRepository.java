package br.com.oficina.domain.repository;

import java.util.List;
import java.util.Optional;

import br.com.oficina.domain.model.orcamento.Orcamento;

public interface OrcamentoRepository {
    void salvar(Orcamento orcamento);

    void atualizar(Orcamento orcamento);

    void excluirPorId(String orcamentoId);

    Optional<Orcamento> buscarPorId(String orcamentoId);

    List<Orcamento> buscarTodos();
}
