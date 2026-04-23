package br.com.oficina.ordemservico.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface OrdemDeServicoRepository {
    void salvar(OrdemDeServico ordemDeServico);

    Optional<OrdemDeServico> buscarPorNumero(String numeroOrdemServico);

    List<OrdemDeServico> buscarPorFiltros(
            String numeroOrdemServico,
            String nomeCliente,
            String placaVeiculo,
            String documentoCliente);

    List<OrdemDeServico> buscarTodas();

    void excluirPorNumero(String numeroOrdemServico);

    boolean existePorFuncionarioId(UUID funcionarioId);
}
