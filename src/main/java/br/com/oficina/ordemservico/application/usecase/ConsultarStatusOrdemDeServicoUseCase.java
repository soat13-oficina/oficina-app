package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.query.ConsultarStatusOrdemDeServicoQuery;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface ConsultarStatusOrdemDeServicoUseCase {
    OrdemDeServico consultarStatus(ConsultarStatusOrdemDeServicoQuery query);
}
