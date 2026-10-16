package br.com.oficina.pecainsumo.application.usecase;

import java.util.Collection;
import java.util.List;

import br.com.oficina.pecainsumo.domain.model.PecaInsumo;

/**
 * Consulta em lote do cadastro de pecas, para outro servico (o Billing, ao gerar o orcamento).
 * Exposta em {@code POST /internal/pecas-insumos/consulta}; vai para a Execution no Passo 4.
 */
public interface ConsultarPecasPorIdsUseCase {

    /** Limite por chamada: um orcamento tem poucas pecas; mais que isso e erro do chamador. */
    int MAXIMO_IDS = 200;

    Resultado consultar(Collection<String> pecaInsumoIds);

    /** @param encontradas na ordem pedida, sem repetir; @param inexistentes ids que nao estao no cadastro */
    record Resultado(List<PecaInsumo> encontradas, List<String> inexistentes) {
    }
}
