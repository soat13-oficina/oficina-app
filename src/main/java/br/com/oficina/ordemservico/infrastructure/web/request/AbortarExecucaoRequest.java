package br.com.oficina.ordemservico.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AbortarExecucaoRequest", description = "Motivo pelo qual o mecânico não pode concluir o serviço.")
public record AbortarExecucaoRequest(
        @Schema(description = "Motivo do aborto", example = "Peça entregue não serve no veículo")
        String motivo) {
}
