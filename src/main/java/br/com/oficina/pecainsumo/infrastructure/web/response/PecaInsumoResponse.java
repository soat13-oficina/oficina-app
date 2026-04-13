package br.com.oficina.pecainsumo.infrastructure.web.response;

import java.math.BigDecimal;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PecaInsumoResponse", description = "Representação de uma peça ou insumo")
public record PecaInsumoResponse(
        String id,
        String descricao,
        String marca,
        BigDecimal preco,
        int quantidadeEstoque,
        int quantidadeReservada,
        int quantidadeDisponivel,
        String codigoReferencia,
        CategoriaPeca categoria) {

    @Override
    @Schema(description = "Identificador da peça/insumo", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
    public String id() {
        return id;
    }

    @Override
    @Schema(description = "Descrição da peça/insumo", example = "Filtro de óleo motor")
    public String descricao() {
        return descricao;
    }

    @Override
    @Schema(description = "Marca da peça/insumo", example = "Bosch")
    public String marca() {
        return marca;
    }

    @Override
    @Schema(description = "Preço unitário", example = "45.90")
    public BigDecimal preco() {
        return preco;
    }

    @Override
    @Schema(description = "Quantidade total em estoque", example = "10")
    public int quantidadeEstoque() {
        return quantidadeEstoque;
    }

    @Override
    @Schema(description = "Quantidade reservada", example = "2")
    public int quantidadeReservada() {
        return quantidadeReservada;
    }

    @Override
    @Schema(description = "Quantidade disponível (estoque - reservada)", example = "8")
    public int quantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    @Override
    @Schema(description = "Código de referência do fabricante", example = "OB0986B01044")
    public String codigoReferencia() {
        return codigoReferencia;
    }

    @Override
    @Schema(description = "Categoria da peça/insumo", example = "FILTROS")
    public CategoriaPeca categoria() {
        return categoria;
    }

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
