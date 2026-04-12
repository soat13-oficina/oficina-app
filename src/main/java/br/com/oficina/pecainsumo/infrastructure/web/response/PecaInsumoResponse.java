package br.com.oficina.pecainsumo.infrastructure.web.response;

import java.math.BigDecimal;

import br.com.oficina.pecainsumo.domain.model.PecaInsumo;

public record PecaInsumoResponse(
        String id,
        String descricao,
        String marca,
        BigDecimal preco,
        int quantidadeEstoque,
        int quantidadeReservada,
        int quantidadeDisponivel,
        String codigoReferencia,
        String categoria) {
    public static PecaInsumoResponse from(PecaInsumo pecaInsumo) {
        return new PecaInsumoResponse(
                pecaInsumo.getId(),
                pecaInsumo.getDescricao(),
                pecaInsumo.getMarca(),
                pecaInsumo.getPreco(),
                pecaInsumo.getQuantidadeEstoque(),
                pecaInsumo.getQuantidadeReservada(),
                pecaInsumo.getQuantidadeDisponivel(),
                pecaInsumo.getCodigoReferencia(),
                pecaInsumo.getCategoria());
    }
}
