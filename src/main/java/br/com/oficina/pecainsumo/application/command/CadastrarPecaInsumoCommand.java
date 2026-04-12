package br.com.oficina.pecainsumo.application.command;

import java.math.BigDecimal;

public record CadastrarPecaInsumoCommand(
        String descricao,
        String marca,
        BigDecimal preco,
        int quantidadeEstoque,
        String codigoReferencia,
        String categoria) {
}
