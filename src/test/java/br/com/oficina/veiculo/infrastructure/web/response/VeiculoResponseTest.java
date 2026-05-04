package br.com.oficina.veiculo.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class VeiculoResponseTest {

    @Test
    void deveConverterVeiculoParaResponse() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);

        VeiculoResponse response = VeiculoResponse.from(veiculo);

        assertEquals("ABC1D23", response.placa());
        assertEquals("Toyota", response.marca());
        assertEquals("Corolla", response.modelo());
        assertEquals("Toyota Motor Corporation", response.fabricante());
        assertEquals(2024, response.ano());
        assertEquals(177, response.potencia());
        assertEquals("AUTOMATICO", response.cambio());
        assertEquals("FLEX", response.tipo());
    }
}
