package br.com.oficina.pecainsumo.application.command;

import java.math.BigDecimal;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;

public record CadastrarPecaInsumoCommand(
        String descricao,
        String marca,
        BigDecimal preco,
        int quantidadeEstoque,
        String codigoReferencia,
        CategoriaPeca categoria) {
}
