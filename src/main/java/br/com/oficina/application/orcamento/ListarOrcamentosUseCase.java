package br.com.oficina.application.orcamento;

import java.util.List;

import br.com.oficina.domain.model.orcamento.Orcamento;

public interface ListarOrcamentosUseCase {
    List<Orcamento> listarOrcamentos();
}
