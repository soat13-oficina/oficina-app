package br.com.oficina.ordemservico.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AlterarOrdemDeServicoRequest", description = "Dados necessários para alterar uma ordem de serviço aberta")
public record AlterarOrdemDeServicoRequest(
        String clienteId,
        String funcionarioId,
        String placaVeiculo) {
    @Override
    @Schema(description = "Identificador UUID do cliente da ordem de serviço", example = "11111111-1111-1111-1111-111111111111")
    public String clienteId() {
        return clienteId;
    }

    @Override
    @Schema(description = "Identificador UUID do funcionário criador da ordem de serviço", example = "22222222-2222-2222-2222-222222222222")
    public String funcionarioId() {
        return funcionarioId;
    }

    @Override
    @Schema(description = "Placa do veículo vinculado à ordem de serviço", example = "ABC1D23")
    public String placaVeiculo() {
        return placaVeiculo;
    }
}
