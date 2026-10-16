package br.com.oficina.orcamento.application.port;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * Descricao e preco atuais das pecas, que sao da Execution (ADR 0003). O orcamento guarda um
 * snapshot do que consultou (ADR 0002).
 *
 * <p>No monolito, o adaptador fica em {@code br.com.oficina.ponte}; no Billing Service vira o
 * cliente REST de {@code POST /internal/pecas-insumos/consulta}.
 */
public interface CatalogoPecas {

    /** Pecas encontradas, por id. Id ausente no mapa = peca inexistente. */
    Map<String, PecaCatalogo> consultar(Collection<String> pecaInsumoIds);

    record PecaCatalogo(String pecaInsumoId, String descricao, BigDecimal preco) {
    }
}
