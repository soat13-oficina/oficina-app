package br.com.oficina.pecainsumo.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class PecaInsumo {
    private final String id;
    private final String descricao;
    private final String marca;
    private final BigDecimal preco;
    private final int quantidadeEstoque;
    private final int quantidadeReservada;
    private final String codigoReferencia;
    private final String categoria;

    public PecaInsumo(
            String id,
            String descricao,
            String marca,
            BigDecimal preco,
            int quantidadeEstoque,
            int quantidadeReservada,
            String codigoReferencia,
            String categoria) {
        this.id = id;
        this.descricao = descricao;
        this.marca = marca;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.quantidadeReservada = quantidadeReservada;
        this.codigoReferencia = codigoReferencia;
        this.categoria = categoria;
    }

    public PecaInsumo(
            String descricao,
            String marca,
            BigDecimal preco,
            int quantidadeEstoque,
            String codigoReferencia,
            String categoria) {
        this(UUID.randomUUID().toString(), descricao, marca, preco, quantidadeEstoque, 0, codigoReferencia, categoria);
    }

    public String getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getMarca() {
        return marca;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public int getQuantidadeReservada() {
        return quantidadeReservada;
    }

    public int getQuantidadeDisponivel() {
        return quantidadeEstoque - quantidadeReservada;
    }

    public String getCodigoReferencia() {
        return codigoReferencia;
    }

    public String getCategoria() {
        return categoria;
    }
}
