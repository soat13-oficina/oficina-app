package br.com.oficina.pecainsumo.application.command;

import java.math.BigDecimal;

public record AlterarPecaInsumoCommand(
        String id,
        String descricao,
        String marca,
        BigDecimal preco,
        int quantidadeEstoque,
        int quantidadeReservada,
        String codigoReferencia,
        String categoria) {
}
