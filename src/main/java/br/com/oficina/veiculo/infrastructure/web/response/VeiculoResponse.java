package br.com.oficina.veiculo.infrastructure.web.response;

import java.util.UUID;

import br.com.oficina.veiculo.domain.model.Veiculo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VeiculoResponse", description = "Representação de um veículo")
public record VeiculoResponse(
        UUID clienteId,
        String placa,
        String marca,
        String modelo,
        String fabricante,
        int ano,
        int potencia,
        String cambio,
        String tipo) {
    @Override
    @Schema(description = "Identificador UUID do cliente proprietário", example = "11111111-1111-1111-1111-111111111111")
    public UUID clienteId() {
        return clienteId;
    }

    @Override
    @Schema(description = "Placa normalizada do veículo", example = "ABC1D23")
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
    public String tipo() {
        return tipo;
    }

    public static VeiculoResponse from(Veiculo veiculo) {
        return new VeiculoResponse(
                veiculo.getClienteId(),
                veiculo.getPlaca(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getFabricante(),
                veiculo.getAno(),
                veiculo.getPotencia(),
                veiculo.getCambio(),
                veiculo.getTipo().name());
    }
}
