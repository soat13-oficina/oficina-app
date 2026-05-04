package br.com.oficina.pecainsumo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CategoriaPecaTest {

    @Test
    void deveConterDezCategorias() {
        assertEquals(10, CategoriaPeca.values().length);
    }

    @ParameterizedTest
    @EnumSource(CategoriaPeca.class)
    void deveRetornarCodigoEDescricaoParaTodasAsCategorias(CategoriaPeca categoria) {
        assertEquals(true, categoria.getCodigo() > 0);
        assertEquals(true, !categoria.getDescricao().isBlank());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeFiltros() {
        assertEquals(1, CategoriaPeca.FILTROS.getCodigo());
        assertEquals("Filtros", CategoriaPeca.FILTROS.getDescricao());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeFreios() {
        assertEquals(2, CategoriaPeca.FREIOS.getCodigo());
        assertEquals("Freios", CategoriaPeca.FREIOS.getDescricao());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeLubrificantes() {
        assertEquals(3, CategoriaPeca.LUBRIFICANTES.getCodigo());
        assertEquals("Lubrificantes", CategoriaPeca.LUBRIFICANTES.getDescricao());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeEletrica() {
        assertEquals(4, CategoriaPeca.ELETRICA.getCodigo());
        assertEquals("Elétrica", CategoriaPeca.ELETRICA.getDescricao());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeTransmissao() {
        assertEquals(5, CategoriaPeca.TRANSMISSAO.getCodigo());
        assertEquals("Transmissão", CategoriaPeca.TRANSMISSAO.getDescricao());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeIgnicao() {
        assertEquals(6, CategoriaPeca.IGNICAO.getCodigo());
        assertEquals("Ignição", CategoriaPeca.IGNICAO.getDescricao());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeSuspensao() {
        assertEquals(7, CategoriaPeca.SUSPENSAO.getCodigo());
        assertEquals("Suspensão", CategoriaPeca.SUSPENSAO.getDescricao());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeArrefecimento() {
        assertEquals(8, CategoriaPeca.ARREFECIMENTO.getCodigo());
        assertEquals("Arrefecimento", CategoriaPeca.ARREFECIMENTO.getDescricao());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeCombustivel() {
        assertEquals(9, CategoriaPeca.COMBUSTIVEL.getCodigo());
        assertEquals("Combustível", CategoriaPeca.COMBUSTIVEL.getDescricao());
    }

    @Test
    void deveRetornarCodigoEDescricaoCorretosDeEscapamento() {
        assertEquals(10, CategoriaPeca.ESCAPAMENTO.getCodigo());
        assertEquals("Escapamento", CategoriaPeca.ESCAPAMENTO.getDescricao());
    }

    @Test
    void deveFalharAoConverterValorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> CategoriaPeca.valueOf("INEXISTENTE"));
    }
}
