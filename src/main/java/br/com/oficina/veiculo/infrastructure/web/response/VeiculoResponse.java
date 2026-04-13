package br.com.oficina.veiculo.infrastructure.web.response;

import java.util.UUID;

import br.com.oficina.veiculo.domain.model.Veiculo;

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
