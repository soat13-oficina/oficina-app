package br.com.oficina.pecainsumo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PecaInsumoTest {

    @Test
    void deveExporDadosDaPecaInsumoComTodosOsCampos() {
        PecaInsumo peca = new PecaInsumo(
                "id-1",
                "Filtro de óleo",
                "Bosch",
                new BigDecimal("45.90"),
                10,
                2,
                "OB0986B01044",
                CategoriaPeca.FILTROS);

        assertEquals("id-1", peca.getId());
        assertEquals("Filtro de óleo", peca.getDescricao());
        assertEquals("Bosch", peca.getMarca());
        assertEquals(new BigDecimal("45.90"), peca.getPreco());
        assertEquals(10, peca.getQuantidadeEstoque());
        assertEquals(2, peca.getQuantidadeReservada());
        assertEquals("OB0986B01044", peca.getCodigoReferencia());
        assertEquals(CategoriaPeca.FILTROS, peca.getCategoria());
    }

    @Test
    void deveCalcularQuantidadeDisponivelCorretamente() {
        PecaInsumo peca = new PecaInsumo(
                "id-2",
                "Pastilha de freio",
                "TRW",
                new BigDecimal("120.00"),
                20,
                5,
                "TRW001",
                CategoriaPeca.FREIOS);

        assertEquals(15, peca.getQuantidadeDisponivel());
    }

    @Test
    void deveRetornarQuantidadeDisponivelIgualAoEstoqueQuandoNaoHouverReserva() {
        PecaInsumo peca = new PecaInsumo(
                "id-3",
                "Óleo lubrificante",
                "Mobil",
                new BigDecimal("35.00"),
                8,
                0,
                "MOB5W30",
                CategoriaPeca.LUBRIFICANTES);

        assertEquals(8, peca.getQuantidadeDisponivel());
        assertEquals(peca.getQuantidadeEstoque(), peca.getQuantidadeDisponivel());
    }

    @Test
    void deveCriarPecaInsumoComConstrutorSimplificadoSemReserva() {
        PecaInsumo peca = new PecaInsumo(
                "Vela de ignição",
                "NGK",
                new BigDecimal("28.50"),
                15,
                "NGK001",
                CategoriaPeca.IGNICAO);

        assertNotNull(peca.getId());
        assertEquals("Vela de ignição", peca.getDescricao());
        assertEquals("NGK", peca.getMarca());
        assertEquals(new BigDecimal("28.50"), peca.getPreco());
        assertEquals(15, peca.getQuantidadeEstoque());
        assertEquals(0, peca.getQuantidadeReservada());
        assertEquals(15, peca.getQuantidadeDisponivel());
        assertEquals("NGK001", peca.getCodigoReferencia());
        assertEquals(CategoriaPeca.IGNICAO, peca.getCategoria());
    }

    @Test
    void deveGerarIdAutomaticoNoConstrutorSimplificado() {
        PecaInsumo peca1 = new PecaInsumo(
                "Amortecedor dianteiro",
                "Monroe",
                new BigDecimal("250.00"),
                4,
                "MNR001",
                CategoriaPeca.SUSPENSAO);

        PecaInsumo peca2 = new PecaInsumo(
                "Amortecedor traseiro",
                "Monroe",
                new BigDecimal("220.00"),
                6,
                "MNR002",
                CategoriaPeca.SUSPENSAO);

        assertNotNull(peca1.getId());
        assertNotNull(peca2.getId());
    }
}
