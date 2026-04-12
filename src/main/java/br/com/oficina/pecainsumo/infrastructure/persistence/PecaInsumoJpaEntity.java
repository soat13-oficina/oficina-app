package br.com.oficina.pecainsumo.infrastructure.persistence;

import java.math.BigDecimal;

import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pecas_insumos")
public class PecaInsumoJpaEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private int quantidadeEstoque;

    @Column(nullable = false)
    private int quantidadeReservada;

    @Column(nullable = false)
    private String codigoReferencia;

    @Column(nullable = false)
    private String categoria;

    protected PecaInsumoJpaEntity() {
    }

    private PecaInsumoJpaEntity(
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

    public static PecaInsumoJpaEntity fromDomain(PecaInsumo pecaInsumo) {
        return new PecaInsumoJpaEntity(
                pecaInsumo.getId(),
                pecaInsumo.getDescricao(),
                pecaInsumo.getMarca(),
                pecaInsumo.getPreco(),
                pecaInsumo.getQuantidadeEstoque(),
                pecaInsumo.getQuantidadeReservada(),
                pecaInsumo.getCodigoReferencia(),
                pecaInsumo.getCategoria());
    }

    public PecaInsumo toDomain() {
        return new PecaInsumo(
                id,
                descricao,
                marca,
                preco,
                quantidadeEstoque,
                quantidadeReservada,
                codigoReferencia,
                categoria);
    }
}
