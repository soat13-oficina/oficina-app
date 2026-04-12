package br.com.oficina.ordemservico.infrastructure.web.response;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;

public record OrdemDeServicoResponse(
        String id,
        String numeroOrdemServico,
        String clienteId,
        String nomeCliente,
        String cpfCliente,
        String placaVeiculo,
        StatusOrdemDeServico status) {
    public static OrdemDeServicoResponse from(OrdemDeServico ordemDeServico) {
        return new OrdemDeServicoResponse(
                ordemDeServico.getId(),
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getCliente().getId(),
                ordemDeServico.getCliente().getNome(),
                ordemDeServico.getCliente().getCpf(),
                ordemDeServico.getVeiculo().getPlaca(),
                ordemDeServico.getStatus());
    }
}
