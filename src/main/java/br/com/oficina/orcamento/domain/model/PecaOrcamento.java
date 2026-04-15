package br.com.oficina.orcamento.domain.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PecaOrcamento {
    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "preco", nullable = false, precision = 19, scale = 2)
    private BigDecimal preco;

    protected PecaOrcamento() {
    }

    public PecaOrcamento(String descricao, BigDecimal preco) {
        this.descricao = descricao;
        this.preco = preco;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }
}
