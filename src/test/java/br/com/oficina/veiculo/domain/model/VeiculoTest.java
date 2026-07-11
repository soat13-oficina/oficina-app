package br.com.oficina.veiculo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.Year;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;

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

    @Test
    void deveNormalizarPlacaRemovendoEspacosEHifensEAplicandoCaixaAlta() {
        Veiculo veiculo = new Veiculo(
                " abc-1d23 ",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);

        assertEquals("ABC1D23", veiculo.getPlaca());
    }

    @Test
    void deveAceitarFormatoAntigoEMercosul() {
        Veiculo formatoAntigo = new Veiculo(
                "ABC-1234",
                "Ford",
                "Ka",
                "Ford Motor Company",
                2020,
                85,
                "MANUAL",
                TipoCombustivel.FLEX);
        Veiculo formatoMercosul = new Veiculo(
                "ABC1D23",
                "Fiat",
                "Pulse",
                "Fiat",
                2024,
                130,
                "AUTOMATICO",
                TipoCombustivel.FLEX);

        assertEquals("ABC1234", formatoAntigo.getPlaca());
        assertEquals("ABC1D23", formatoMercosul.getPlaca());
    }

    @Test
    void deveFalharQuandoPlacaForInvalida() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new Veiculo(
                        "AB12",
                        "Toyota",
                        "Corolla",
                        "Toyota Motor Corporation",
                        2024,
                        177,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX));

        assertEquals("Placa do veiculo invalida", exception.getMessage());
    }

    @Test
    void deveAceitarAnoNosLimitesValidos() {
        int anoMaximo = Year.now().getValue() + 1;

        assertDoesNotThrow(() -> new Veiculo(
                "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                1886, 1, "AUTOMATICO", TipoCombustivel.FLEX));
        assertDoesNotThrow(() -> new Veiculo(
                "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                anoMaximo, 177, "AUTOMATICO", TipoCombustivel.FLEX));
    }

    @Test
    void deveFalharQuandoAnoForMenorQueOMinimo() {
        int anoMaximo = Year.now().getValue() + 1;

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new Veiculo(
                        "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                        1885, 177, "AUTOMATICO", TipoCombustivel.FLEX));

        assertEquals("Ano do veiculo deve estar entre 1886 e " + anoMaximo + ".", exception.getMessage());
    }

    @Test
    void deveFalharQuandoAnoForMaiorQueOMaximo() {
        int anoMaximo = Year.now().getValue() + 1;

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new Veiculo(
                        "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                        anoMaximo + 1, 177, "AUTOMATICO", TipoCombustivel.FLEX));

        assertEquals("Ano do veiculo deve estar entre 1886 e " + anoMaximo + ".", exception.getMessage());
    }

    @Test
    void deveFalharQuandoPotenciaNaoForPositiva() {
        RegraDeNegocioException zero = assertThrows(
                RegraDeNegocioException.class,
                () -> new Veiculo(
                        "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                        2024, 0, "AUTOMATICO", TipoCombustivel.FLEX));
        assertEquals("Potencia do veiculo deve ser maior que zero.", zero.getMessage());

        RegraDeNegocioException negativa = assertThrows(
                RegraDeNegocioException.class,
                () -> new Veiculo(
                        "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                        2024, -100, "AUTOMATICO", TipoCombustivel.FLEX));
        assertEquals("Potencia do veiculo deve ser maior que zero.", negativa.getMessage());
    }

    @Test
    void deveValidarAnoEPotenciaAoAlterar() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation",
                2024, 177, "AUTOMATICO", TipoCombustivel.FLEX);

        RegraDeNegocioException anoInvalido = assertThrows(
                RegraDeNegocioException.class,
                () -> veiculo.alterar("Toyota", "Corolla", "Toyota Motor Corporation",
                        1885, 177, "AUTOMATICO", TipoCombustivel.FLEX));
        assertEquals("Ano do veiculo deve estar entre 1886 e " + (Year.now().getValue() + 1) + ".",
                anoInvalido.getMessage());

        RegraDeNegocioException potenciaInvalida = assertThrows(
                RegraDeNegocioException.class,
                () -> veiculo.alterar("Toyota", "Corolla", "Toyota Motor Corporation",
                        2024, 0, "AUTOMATICO", TipoCombustivel.FLEX));
        assertEquals("Potencia do veiculo deve ser maior que zero.", potenciaInvalida.getMessage());
    }
}
