package br.com.oficina.ponte;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import br.com.oficina.ordemservico.application.port.CatalogoPecas;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

/**
 * Cadastro de pecas (estoque, futura Execution) consultado pela OS: existencia das pecas do
 * diagnostico. Transitorio: ver {@code package-info}. (O Billing consulta precos pelo REST interno
 * {@code POST /internal/pecas-insumos/consulta}.)
 */
@Component
public class CatalogoDePecasDoEstoque implements CatalogoPecas {

    private final PecaInsumoRepository pecas;

    public CatalogoDePecasDoEstoque(PecaInsumoRepository pecas) {
        this.pecas = pecas;
    }

    @Override
    public Set<String> inexistentes(Collection<String> pecaInsumoIds) {
        Set<String> inexistentes = new LinkedHashSet<>();
        for (String id : new LinkedHashSet<>(pecaInsumoIds)) {
            if (pecas.buscarPorId(id).isEmpty()) {
                inexistentes.add(id);
            }
        }
        return inexistentes;
    }
}
