package br.com.oficina.ordemservico.infrastructure.web.response;

import java.time.LocalDateTime;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;

public record AcompanhamentoOrdemDeServicoResponse(
        String numeroOrdemServico,
        String nomeCliente,
        String placaVeiculo,
        StatusOrdemDeServico status,
        LocalDateTime iniciadaEm,
        LocalDateTime finalizadaEm) {
    public static AcompanhamentoOrdemDeServicoResponse from(OrdemDeServico ordemDeServico) {
        return new AcompanhamentoOrdemDeServicoResponse(
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getCliente().getNome(),
                ordemDeServico.getVeiculo().getPlaca(),
                ordemDeServico.getStatus(),
                ordemDeServico.getIniciadaEm(),
                ordemDeServico.getFinalizadaEm());
    }
}
