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
    private final CategoriaPeca categoria;

    public PecaInsumo(
            String id,
            String descricao,
            String marca,
            BigDecimal preco,
            int quantidadeEstoque,
            int quantidadeReservada,
            String codigoReferencia,
            CategoriaPeca categoria) {
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
            CategoriaPeca categoria) {
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

    public CategoriaPeca getCategoria() {
        return categoria;
    }

    /** Reserva para uma OS (passo ReservarPecas da Saga). Sem disponivel suficiente e erro de programacao: confira antes. */
    public PecaInsumo reservar(int quantidade) {
        if (quantidade <= 0 || quantidade > getQuantidadeDisponivel()) {
            throw new IllegalArgumentException("Reserva invalida: solicitado " + quantidade + ", disponivel " + getQuantidadeDisponivel());
        }
        return comQuantidades(quantidadeEstoque, quantidadeReservada + quantidade);
    }

    /** Devolve ao disponivel o que estava reservado (compensacao LiberarReserva). Nunca deixa a reserva negativa. */
    public PecaInsumo liberarReserva(int quantidade) {
        return comQuantidades(quantidadeEstoque, Math.max(0, quantidadeReservada - quantidade));
    }

    /** Baixa definitiva de pecas reservadas, na conclusao do servico. Nunca deixa estoque ou reserva negativos. */
    public PecaInsumo consumirReservada(int quantidade) {
        return comQuantidades(Math.max(0, quantidadeEstoque - quantidade), Math.max(0, quantidadeReservada - quantidade));
    }

    private PecaInsumo comQuantidades(int estoque, int reservada) {
        return new PecaInsumo(id, descricao, marca, preco, estoque, reservada, codigoReferencia, categoria);
    }
}
