package br.com.oficina.ordemservico.domain.repository;

import java.util.List;
import java.util.Optional;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface OrdemDeServicoRepository {
    void salvar(OrdemDeServico ordemDeServico);

    Optional<OrdemDeServico> buscarPorNumero(String numeroOrdemServico);

    List<OrdemDeServico> buscarTodas();
}
