package br.com.oficina.veiculo.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;

@Schema(name = "AlterarVeiculoRequest", description = "Dados necessários para alterar um veículo")
public record AlterarVeiculoRequest(
        String marca,
        String modelo,
        String fabricante,
        int ano,
        int potencia,
        String cambio,
        TipoCombustivel tipo) {
    @Override
    @Schema(description = "Marca do veículo", example = "Toyota")
    public String marca() {
        return marca;
    }

    @Override
    @Schema(description = "Modelo do veículo", example = "Corolla")
    public String modelo() {
        return modelo;
    }

    @Override
    @Schema(description = "Fabricante do veículo", example = "Toyota Motor Corporation")
    public String fabricante() {
        return fabricante;
    }

    @Override
    @Schema(description = "Ano do veículo", example = "2024")
    public int ano() {
        return ano;
    }

    @Override
    @Schema(description = "Potência do veículo em cavalos", example = "177")
    public int potencia() {
        return potencia;
    }

    @Override
    @Schema(description = "Tipo de câmbio do veículo", example = "AUTOMATICO")
    public String cambio() {
        return cambio;
    }

    @Override
    @Schema(description = "Tipo de combustível do veículo", example = "FLEX")
    public TipoCombustivel tipo() {
        return tipo;
    }
}
