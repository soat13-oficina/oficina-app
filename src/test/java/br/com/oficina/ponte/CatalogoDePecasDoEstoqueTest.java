package br.com.oficina.ponte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

/** Adaptador transitorio: a OS consulta o cadastro de pecas sem importar o estoque. */
class CatalogoDePecasDoEstoqueTest {

    private final TestPecaInsumoRepository pecas = new TestPecaInsumoRepository();
    private final CatalogoDePecasDoEstoque catalogo = new CatalogoDePecasDoEstoque(pecas);

    @Test
    void inexistentesSaoOsIdsQueNaoEstaoNoCadastroUmaVezCadaNaOrdemPedida() {
        pecas.salvar(new PecaInsumo("filtro", "Filtro de oleo", "Bosch", new BigDecimal("80.00"), 10, 0, "REF-1", CategoriaPeca.FILTROS));

        assertThat(catalogo.inexistentes(List.of("filtro", "sumiu-1", "sumiu-2", "sumiu-1"))).containsExactly("sumiu-1", "sumiu-2");
        assertThat(catalogo.inexistentes(List.of("filtro"))).isEmpty();
        assertThat(catalogo.inexistentes(List.of())).isEmpty();
    }
}
