package br.com.oficina.ordemservico.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AberturaOrdemDeServicoResponse", description = "Identificação e situação da ordem de serviço recém-aberta")
public record AberturaOrdemDeServicoResponse(
        @Schema(description = "Número único da ordem de serviço", example = "OS-1A2B3C4D")
        String numeroOrdemServico,
        @Schema(description = "Situação de negócio atual da ordem de serviço", example = "Recebida")
        String situacao) {
}
