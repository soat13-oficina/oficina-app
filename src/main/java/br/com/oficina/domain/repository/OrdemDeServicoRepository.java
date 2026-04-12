package br.com.oficina.domain.repository;

import java.util.Optional;

import br.com.oficina.domain.model.ordemservico.OrdemDeServico;

public interface OrdemDeServicoRepository {
    void salvar(OrdemDeServico ordemDeServico);

    Optional<OrdemDeServico> buscarPorId(String ordemDeServicoId);
}
