package br.com.oficina.pecainsumo.infrastructure.web.request;

import java.math.BigDecimal;

public record CadastrarPecaInsumoRequest(
        String descricao,
        String marca,
        BigDecimal preco,
        int quantidadeEstoque,
        String codigoReferencia,
        String categoria) {
}
