package br.com.oficina.ordemservico.application.usecase;

import java.util.List;

import br.com.oficina.ordemservico.application.query.ConsultarOrdensDeServicoQuery;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface ConsultarOrdensDeServicoUseCase {
    List<OrdemDeServico> consultarOrdensDeServico(ConsultarOrdensDeServicoQuery query);
}
