package br.com.oficina.ordemservico.domain.repository;

import java.util.List;
import java.util.UUID;

import br.com.oficina.ordemservico.domain.model.AlteracaoStatusOrdemDeServico;

public interface HistoricoStatusOrdemDeServicoRepository {

    void registrar(UUID ordemDeServicoId, List<AlteracaoStatusOrdemDeServico> alteracoes);

    /** Mudancas da ordem, da mais antiga para a mais recente. */
    List<AlteracaoStatusOrdemDeServico> listarPorOrdem(UUID ordemDeServicoId);
}
