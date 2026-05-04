package br.com.oficina.pecainsumo.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;

class PecaInsumoResponseTest {

    @Test
    void deveConverterPecaInsumoParaResponse() {
        PecaInsumo peca = new PecaInsumo(
                "id-1",
                "Filtro de óleo",
                "Bosch",
                new BigDecimal("45.90"),
                10,
                2,
                "OB0986B01044",
                CategoriaPeca.FILTROS);

        PecaInsumoResponse response = PecaInsumoResponse.from(peca);

        assertEquals("id-1", response.id());
        assertEquals("Filtro de óleo", response.descricao());
        assertEquals("Bosch", response.marca());
        assertEquals(new BigDecimal("45.90"), response.preco());
        assertEquals(10, response.quantidadeEstoque());
        assertEquals(2, response.quantidadeReservada());
        assertEquals(8, response.quantidadeDisponivel());
        assertEquals("OB0986B01044", response.codigoReferencia());
        assertEquals(CategoriaPeca.FILTROS, response.categoria());
    }

    @Test
    void deveCalcularQuantidadeDisponivelCorretamenteNoResponse() {
        PecaInsumo peca = new PecaInsumo(
                "id-2",
                "Pastilha de freio",
                "TRW",
                new BigDecimal("120.00"),
                20,
                0,
                "TRW001",
                CategoriaPeca.FREIOS);

        PecaInsumoResponse response = PecaInsumoResponse.from(peca);

        assertEquals(20, response.quantidadeEstoque());
        assertEquals(0, response.quantidadeReservada());
        assertEquals(20, response.quantidadeDisponivel());
    }
}
