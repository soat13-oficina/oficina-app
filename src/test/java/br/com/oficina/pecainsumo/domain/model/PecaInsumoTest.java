package br.com.oficina.pecainsumo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PecaInsumoTest {

    @Test
    void deveCriarPecaComIdGeradoEQuantidadeDisponivel() {
        PecaInsumo peca = new PecaInsumo(
                "Filtro de oleo",
                "Bosch",
                new BigDecimal("39.90"),
                10,
                "FO-123",
                "Lubrificacao");

        assertNotNull(peca.getId());
        assertEquals("Filtro de oleo", peca.getDescricao());
        assertEquals("Bosch", peca.getMarca());
        assertEquals(new BigDecimal("39.90"), peca.getPreco());
        assertEquals(10, peca.getQuantidadeEstoque());
        assertEquals(0, peca.getQuantidadeReservada());
        assertEquals(10, peca.getQuantidadeDisponivel());
        assertEquals("FO-123", peca.getCodigoReferencia());
        assertEquals("Lubrificacao", peca.getCategoria());
    }

    @Test
    void deveCalcularQuantidadeDisponivelAoReconstituirPeca() {
        PecaInsumo peca = new PecaInsumo(
                "peca-001",
                "Pastilha de freio",
                "Cobreq",
                new BigDecimal("129.50"),
                12,
                5,
                "PF-987",
                "Freio");

        assertEquals("peca-001", peca.getId());
        assertEquals(7, peca.getQuantidadeDisponivel());
    }
}
