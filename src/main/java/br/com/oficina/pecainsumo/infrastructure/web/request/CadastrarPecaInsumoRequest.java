package br.com.oficina.pecainsumo.infrastructure.web.request;

import java.math.BigDecimal;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CadastrarPecaInsumoRequest", description = "Dados necessários para cadastrar uma peça ou insumo")
public record CadastrarPecaInsumoRequest(
        @NotBlank(message = "O campo descricao é obrigatório.")
        String descricao,

        @NotBlank(message = "O campo marca é obrigatório.")
        String marca,

        @NotNull(message = "O campo preco é obrigatório.")
        @DecimalMin(value = "0.01", message = "O preço deve ser no mínimo 0.01.")
        BigDecimal preco,

        @Min(value = 0, message = "A quantidade em estoque não pode ser negativa.")
        int quantidadeEstoque,

        @NotBlank(message = "O campo codigoReferencia é obrigatório.")
        String codigoReferencia,

        @NotNull(message = "O campo categoria é obrigatório.")
        CategoriaPeca categoria) {

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
    @Schema(description = "Preço unitário da peça/insumo", example = "45.90")
    public BigDecimal preco() {
        return preco;
    }

    @Override
    @Schema(description = "Quantidade inicial em estoque", example = "10")
    public int quantidadeEstoque() {
        return quantidadeEstoque;
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
}
