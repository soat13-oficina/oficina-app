package br.com.oficina.veiculo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VeiculoTest {

    @Test
    void deveExporDadosDoVeiculo() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);

        assertEquals("ABC1D23", veiculo.getPlaca());
        assertEquals("Toyota", veiculo.getMarca());
        assertEquals("Corolla", veiculo.getModelo());
        assertEquals("Toyota Motor Corporation", veiculo.getFabricante());
        assertEquals(2024, veiculo.getAno());
        assertEquals(177, veiculo.getPotencia());
        assertEquals("AUTOMATICO", veiculo.getCambio());
        assertEquals(TipoCombustivel.FLEX, veiculo.getTipo());
    }
}
