package br.com.oficina.pecainsumo.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(name = "ConsumirPecaRequest", description = "Dados para consumir peça reservada")
public record ConsumirPecaRequest(
        @Min(value = 1, message = "A quantidade deve ser no mínimo 1.")
        int quantidade) {

    @Override
    @Schema(description = "Quantidade a consumir do estoque reservado", example = "2")
    public int quantidade() {
        return quantidade;
    }
}
