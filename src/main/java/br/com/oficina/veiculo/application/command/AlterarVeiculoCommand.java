package br.com.oficina.veiculo.application.command;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;

public record AlterarVeiculoCommand(
        String placa,
        String marca,
        String modelo,
        String fabricante,
        int ano,
        int potencia,
        String cambio,
        TipoCombustivel tipo) {
}
