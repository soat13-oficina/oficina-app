package br.com.oficina.pecainsumo.application.command;

import java.math.BigDecimal;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;

public record AlterarPecaInsumoCommand(
        String id,
        String descricao,
        String marca,
        BigDecimal preco,
        int quantidadeEstoque,
        int quantidadeReservada,
        String codigoReferencia,
        CategoriaPeca categoria) {
}
