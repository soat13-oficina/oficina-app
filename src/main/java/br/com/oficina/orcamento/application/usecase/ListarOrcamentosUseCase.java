package br.com.oficina.orcamento.application.usecase;

import java.util.List;

import br.com.oficina.orcamento.domain.model.Orcamento;

public interface ListarOrcamentosUseCase {
    List<Orcamento> listarOrcamentos();
}
