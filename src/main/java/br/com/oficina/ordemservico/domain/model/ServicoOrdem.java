package br.com.oficina.ordemservico.domain.model;

import java.math.BigDecimal;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ServicoOrdem {
    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "valor_mao_de_obra", precision = 19, scale = 2)
    private BigDecimal valorMaoDeObra;

    protected ServicoOrdem() {
    }

    public ServicoOrdem(String descricao, BigDecimal valorMaoDeObra) {
        if (descricao == null || descricao.isBlank()) {
            throw new RegraDeNegocioException("Descricao do servico e obrigatoria.");
        }
        if (valorMaoDeObra != null && valorMaoDeObra.signum() < 0) {
            throw new RegraDeNegocioException("Valor da mao de obra nao pode ser negativo.");
        }
        this.descricao = descricao;
        this.valorMaoDeObra = valorMaoDeObra;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValorMaoDeObra() {
        return valorMaoDeObra;
    }
}
