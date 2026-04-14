package br.com.oficina.ordemservico.infrastructure.web.response;

import java.time.LocalDateTime;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AcompanhamentoOrdemDeServicoResponse", description = "Visão simplificada do andamento de uma ordem de serviço para acompanhamento do cliente")
public record AcompanhamentoOrdemDeServicoResponse(
        String numeroOrdemServico,
        String nomeCliente,
        String placaVeiculo,
        StatusOrdemDeServico status,
        LocalDateTime iniciadaEm,
        LocalDateTime finalizadaEm) {
    @Override
    @Schema(description = "Número da ordem de serviço", example = "OS-1A2B3C4D")
    public String numeroOrdemServico() {
        return numeroOrdemServico;
    }

    @Override
    @Schema(description = "Nome do cliente vinculado", example = "Maria da Silva")
    public String nomeCliente() {
        return nomeCliente;
    }

    @Override
    @Schema(description = "Placa do veículo vinculado", example = "ABC1D23")
    public String placaVeiculo() {
        return placaVeiculo;
    }

    @Override
    @Schema(description = "Status atual da ordem de serviço", example = "DIAGNOSTICO_EM_ANDAMENTO")
    public StatusOrdemDeServico status() {
        return status;
    }

    @Override
    @Schema(description = "Data e hora de início do diagnóstico", example = "2030-01-01T10:00:00")
    public LocalDateTime iniciadaEm() {
        return iniciadaEm;
    }

    @Override
    @Schema(description = "Data e hora de finalização da ordem", example = "2030-01-02T18:30:00")
    public LocalDateTime finalizadaEm() {
        return finalizadaEm;
    }

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
