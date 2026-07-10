package br.com.oficina.ordemservico.domain.model;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PecaPrevistaOrdem {
    @Column(name = "peca_insumo_id", nullable = false)
    private String pecaInsumoId;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    protected PecaPrevistaOrdem() {
    }

    public PecaPrevistaOrdem(String pecaInsumoId, int quantidade) {
        if (pecaInsumoId == null || pecaInsumoId.isBlank()) {
            throw new RegraDeNegocioException("Identificador da peca prevista e obrigatorio.");
        }
        if (quantidade <= 0) {
            throw new RegraDeNegocioException("A quantidade da peca prevista deve ser maior que zero.");
        }
        this.pecaInsumoId = pecaInsumoId;
        this.quantidade = quantidade;
    }

    public String getPecaInsumoId() {
        return pecaInsumoId;
    }

    public int getQuantidade() {
        return quantidade;
    }
}
