package br.com.oficina.ordemservico.application.usecase;

import java.util.List;

import br.com.oficina.ordemservico.application.query.ConsultarHistoricoOrdemDeServicoQuery;
import br.com.oficina.ordemservico.domain.model.AlteracaoStatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface ConsultarHistoricoOrdemDeServicoUseCase {

    HistoricoOrdemDeServico consultarHistorico(ConsultarHistoricoOrdemDeServicoQuery query);

    /** A ordem (estado atual) e suas mudancas de status, da mais antiga para a mais recente. */
    record HistoricoOrdemDeServico(OrdemDeServico ordemDeServico, List<AlteracaoStatusOrdemDeServico> alteracoes) {
    }
}
