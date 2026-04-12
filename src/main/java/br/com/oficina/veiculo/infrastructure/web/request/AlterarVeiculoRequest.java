package br.com.oficina.veiculo.infrastructure.web.request;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;

public record AlterarVeiculoRequest(
        String marca,
        String modelo,
        String fabricante,
        int ano,
        int potencia,
        String cambio,
        TipoCombustivel tipo) {
}
