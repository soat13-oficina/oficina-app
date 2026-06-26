package br.com.oficina.veiculo.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;

@Schema(name = "CadastrarVeiculoRequest", description = "Dados necessários para cadastrar um veículo")
public record CadastrarVeiculoRequest(
        @NotBlank String placa,
        @NotBlank String marca,
        @NotBlank String modelo,
        @NotBlank String fabricante,
        @Min(value = 1886, message = "Ano do veiculo deve ser maior ou igual a 1886.") int ano,
        @Positive(message = "Potencia do veiculo deve ser maior que zero.") int potencia,
        @NotBlank String cambio,
        @NotNull TipoCombustivel tipo,
        @NotBlank String clienteId) {
    @Override
    @Schema(description = "Placa do veículo", example = "ABC1D23")
    public String placa() {
        return placa;
    }

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

    @Override
    @Schema(description = "Identificador UUID do cliente proprietário do veículo", example = "11111111-1111-1111-1111-111111111111")
    public String clienteId() {
        return clienteId;
    }
}
