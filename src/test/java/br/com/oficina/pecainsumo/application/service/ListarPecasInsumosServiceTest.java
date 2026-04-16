package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.oficina.pecainsumo.application.query.ListarPecasInsumosQuery;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class ListarPecasInsumosServiceTest {

    private TestPecaInsumoRepository repository;
    private ListarPecasInsumosService service;

    @BeforeEach
    void setUp() {
        repository = new TestPecaInsumoRepository();
        service = new ListarPecasInsumosService(repository);

        repository.salvar(new PecaInsumo("id-1", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS));
        repository.salvar(new PecaInsumo("id-2", "Pastilha de freio", "TRW",
                new BigDecimal("120.00"), 20, 5, "TRW001", CategoriaPeca.FREIOS));
        repository.salvar(new PecaInsumo("id-3", "Filtro de ar", "Bosch",
                new BigDecimal("35.00"), 15, 0, "OB002", CategoriaPeca.FILTROS));
        repository.salvar(new PecaInsumo("id-4", "Óleo lubrificante", "Mobil",
                new BigDecimal("30.00"), 50, 10, "MOB001", CategoriaPeca.LUBRIFICANTES));
    }

    @Test
    void deveListarTodasAsPecasInsumosQuandoSemFiltro() {
        List<PecaInsumo> resultado = service.listarPecasInsumos(
                new ListarPecasInsumosQuery(null, null, null));

        assertEquals(4, resultado.size());
    }

    @Test
    void deveListarPecasInsumosFiltrandoPorMarca() {
        List<PecaInsumo> resultado = service.listarPecasInsumos(
                new ListarPecasInsumosQuery("Bosch", null, null));

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(p -> p.getMarca().equals("Bosch")));
    }

    @Test
    void deveListarPecasInsumosFiltrandoPorCategoria() {
        List<PecaInsumo> resultado = service.listarPecasInsumos(
                new ListarPecasInsumosQuery(null, CategoriaPeca.FILTROS, null));

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(p -> p.getCategoria().equals(CategoriaPeca.FILTROS)));
    }

    @Test
    void deveListarPecasInsumosComReserva() {
        List<PecaInsumo> resultado = service.listarPecasInsumos(
                new ListarPecasInsumosQuery(null, null, true));

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(p -> p.getQuantidadeReservada() > 0));
    }

    @Test
    void deveListarPecasInsumosSemReserva() {
        List<PecaInsumo> resultado = service.listarPecasInsumos(
                new ListarPecasInsumosQuery(null, null, false));

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(p -> p.getQuantidadeReservada() == 0));
    }

    @Test
    void deveListarPecasInsumosFiltrandoPorMarcaECategoria() {
        List<PecaInsumo> resultado = service.listarPecasInsumos(
                new ListarPecasInsumosQuery("Bosch", CategoriaPeca.FILTROS, null));

        assertEquals(2, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoNenhumaPecaCorresponderAoFiltro() {
        List<PecaInsumo> resultado = service.listarPecasInsumos(
                new ListarPecasInsumosQuery("MarcaInexistente", null, null));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveRetornarListaVaziaQuandoRepositorioEstiverVazio() {
        TestPecaInsumoRepository repositorioVazio = new TestPecaInsumoRepository();
        ListarPecasInsumosService servicoVazio = new ListarPecasInsumosService(repositorioVazio);

        List<PecaInsumo> resultado = servicoVazio.listarPecasInsumos(
                new ListarPecasInsumosQuery(null, null, null));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveListarFiltrandoPorMarcaCategoriaEReserva() {
        List<PecaInsumo> resultado = service.listarPecasInsumos(
                new ListarPecasInsumosQuery("TRW", CategoriaPeca.FREIOS, true));

        assertEquals(1, resultado.size());
        assertEquals("Pastilha de freio", resultado.get(0).getDescricao());
    }
}
