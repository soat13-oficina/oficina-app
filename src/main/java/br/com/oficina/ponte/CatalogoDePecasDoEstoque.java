package br.com.oficina.ponte;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.oficina.orcamento.application.port.CatalogoPecas.PecaCatalogo;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

/**
 * Cadastro de pecas (estoque, futura Execution) consultado pelo orcamento (precos) e pela OS
 * (existencia das pecas do diagnostico). Transitorio: ver {@code package-info}.
 */
@Component
public class CatalogoDePecasDoEstoque implements br.com.oficina.orcamento.application.port.CatalogoPecas,
        br.com.oficina.ordemservico.application.port.CatalogoPecas {

    private final PecaInsumoRepository pecas;

    public CatalogoDePecasDoEstoque(PecaInsumoRepository pecas) {
        this.pecas = pecas;
    }

    @Override
    public Map<String, PecaCatalogo> consultar(Collection<String> pecaInsumoIds) {
        Map<String, PecaCatalogo> encontradas = new LinkedHashMap<>();
        for (String id : new LinkedHashSet<>(pecaInsumoIds)) {
            pecas.buscarPorId(id).ifPresent(peca -> encontradas.put(id, paraCatalogo(peca)));
        }
        return encontradas;
    }

    @Override
    public Set<String> inexistentes(Collection<String> pecaInsumoIds) {
        Set<String> inexistentes = new LinkedHashSet<>(pecaInsumoIds);
        inexistentes.removeAll(consultar(pecaInsumoIds).keySet());
        return inexistentes;
    }

    private static PecaCatalogo paraCatalogo(PecaInsumo peca) {
        return new PecaCatalogo(peca.getId(), peca.getDescricao(), peca.getPreco());
    }
}
