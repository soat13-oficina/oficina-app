package br.com.oficina.orcamento.application.usecase;

import java.util.List;

import br.com.oficina.orcamento.application.query.ConsultarOrcamentoQuery;
import br.com.oficina.orcamento.domain.model.Orcamento;

public interface ConsultarOrcamentoUseCase {
    List<Orcamento> consultarOrcamento(ConsultarOrcamentoQuery query);
}
