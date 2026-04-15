package br.com.oficina.veiculo.application.query;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;

public record ConsultarVeiculosQuery(
        String placa,
        Integer ano,
        String marca,
        String fabricante,
        Integer potencia,
        String cambio,
        TipoCombustivel tipo) {
}
