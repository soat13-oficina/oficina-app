package br.com.oficina.ponte;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.oficina.orcamento.application.port.CatalogoPecas.PecaCatalogo;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

/** Adaptador transitorio: o orcamento e a OS consultam o cadastro de pecas sem importar o estoque. */
class CatalogoDePecasDoEstoqueTest {

    private final TestPecaInsumoRepository pecas = new TestPecaInsumoRepository();
    private final CatalogoDePecasDoEstoque catalogo = new CatalogoDePecasDoEstoque(pecas);

    @Test
    void consultaDevolveDescricaoEPrecoDasPecasEncontradasUmaVezCada() {
        pecas.salvar(new PecaInsumo("filtro", "Filtro de oleo", "Bosch", new BigDecimal("80.00"), 10, 0, "REF-1", CategoriaPeca.FILTROS));
        pecas.salvar(new PecaInsumo("oleo", "Oleo 5W30", "Mobil", new BigDecimal("45.90"), 10, 0, "REF-2", CategoriaPeca.FILTROS));

        var encontradas = catalogo.consultar(List.of("oleo", "filtro", "sumiu", "oleo"));

        assertThat(encontradas).containsOnlyKeys("oleo", "filtro");
        assertThat(encontradas.keySet()).as("na ordem pedida").containsExactly("oleo", "filtro");
        assertThat(encontradas.get("filtro")).isEqualTo(new PecaCatalogo("filtro", "Filtro de oleo", new BigDecimal("80.00")));
    }

    @Test
    void inexistentesSaoOsIdsQueNaoEstaoNoCadastro() {
        pecas.salvar(new PecaInsumo("filtro", "Filtro de oleo", "Bosch", new BigDecimal("80.00"), 10, 0, "REF-1", CategoriaPeca.FILTROS));

        assertThat(catalogo.inexistentes(List.of("filtro", "sumiu-1", "sumiu-2", "sumiu-1"))).containsExactly("sumiu-1", "sumiu-2");
        assertThat(catalogo.inexistentes(List.of("filtro"))).isEmpty();
        assertThat(catalogo.inexistentes(List.of())).isEmpty();
    }
}
