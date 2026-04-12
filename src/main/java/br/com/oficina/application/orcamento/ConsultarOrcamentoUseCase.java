package br.com.oficina.application.orcamento;

import java.util.Optional;

import br.com.oficina.domain.model.orcamento.Orcamento;

public interface ConsultarOrcamentoUseCase {
    Optional<Orcamento> consultarOrcamento(ConsultarOrcamentoRequest request);

    record ConsultarOrcamentoRequest(String orcamentoId) {
    }
}
