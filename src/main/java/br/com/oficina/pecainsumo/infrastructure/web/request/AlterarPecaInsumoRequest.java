package br.com.oficina.pecainsumo.infrastructure.web.request;

import java.math.BigDecimal;

public record AlterarPecaInsumoRequest(
        String descricao,
        String marca,
        BigDecimal preco,
        int quantidadeEstoque,
        int quantidadeReservada,
        String codigoReferencia,
        String categoria) {
}
