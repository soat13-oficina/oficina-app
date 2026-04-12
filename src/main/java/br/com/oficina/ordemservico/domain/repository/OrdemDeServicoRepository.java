package br.com.oficina.ordemservico.domain.repository;

import java.util.Optional;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface OrdemDeServicoRepository {
    void salvar(OrdemDeServico ordemDeServico);

    Optional<OrdemDeServico> buscarPorId(String ordemDeServicoId);
}
