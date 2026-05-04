package br.com.oficina.pecainsumo.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(name = "RemoverEstoquePecaRequest", description = "Dados para remover estoque")
public record RemoverEstoquePecaRequest(
        @Min(value = 1, message = "A quantidade deve ser no mínimo 1.")
        int quantidade) {

    @Override
    @Schema(description = "Quantidade a remover do estoque", example = "3")
    public int quantidade() {
        return quantidade;
    }
}
