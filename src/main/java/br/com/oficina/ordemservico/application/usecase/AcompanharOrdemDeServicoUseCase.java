package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.query.AcompanharOrdemDeServicoQuery;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface AcompanharOrdemDeServicoUseCase {
    OrdemDeServico acompanhar(AcompanharOrdemDeServicoQuery query);
}
