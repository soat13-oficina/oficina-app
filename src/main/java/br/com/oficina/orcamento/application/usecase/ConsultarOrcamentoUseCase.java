package br.com.oficina.orcamento.application.usecase;

import java.util.Optional;

import br.com.oficina.orcamento.application.query.ConsultarOrcamentoQuery;
import br.com.oficina.orcamento.domain.model.Orcamento;

public interface ConsultarOrcamentoUseCase {
    Optional<Orcamento> consultarOrcamento(ConsultarOrcamentoQuery query);
}
