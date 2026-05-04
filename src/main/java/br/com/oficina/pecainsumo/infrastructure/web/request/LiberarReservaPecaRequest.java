package br.com.oficina.pecainsumo.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(name = "LiberarReservaPecaRequest", description = "Dados para liberar reserva de peça")
public record LiberarReservaPecaRequest(
        @Min(value = 1, message = "A quantidade deve ser no mínimo 1.")
        int quantidade) {

    @Override
    @Schema(description = "Quantidade a liberar da reserva", example = "1")
    public int quantidade() {
        return quantidade;
    }
}
