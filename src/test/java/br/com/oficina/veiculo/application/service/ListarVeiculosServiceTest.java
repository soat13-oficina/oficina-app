package br.com.oficina.veiculo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.oficina.veiculo.application.query.ListarVeiculosQuery;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.support.persistence.TestVeiculoRepository;

class ListarVeiculosServiceTest {
    private TestVeiculoRepository repository;
    private ListarVeiculosService service;

    @BeforeEach
    void setUp() {
        repository = new TestVeiculoRepository();
        service = new ListarVeiculosService(repository);
        repository.salvar(new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        repository.salvar(new Veiculo(
                "TES1A23",
                "Tesla",
                "Model 3",
                "Tesla Inc.",
                2023,
                283,
                "AUTOMATICO",
                TipoCombustivel.ELETRICO));
        repository.salvar(new Veiculo(
                "FOR1D23",
                "Ford",
                "Ranger",
                "Ford Motor Company",
                2022,
                213,
                "AUTOMATICO",
                TipoCombustivel.DIESEL));
    }

    @Test
    void deveListarTodosQuandoNaoHaFiltros() {
        List<Veiculo> veiculos = service.listarVeiculos(new ListarVeiculosQuery(null, null, null, null, null, null));

        assertEquals(3, veiculos.size());
    }

    @Test
    void deveFiltrarPorAnoMarcaFabricantePotenciaCambioETipo() {
        List<Veiculo> veiculos = service.listarVeiculos(new ListarVeiculosQuery(
                2024,
                "Toyota",
                "Toyota Motor Corporation",
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX));

        assertEquals(1, veiculos.size());
        assertEquals("ABC1D23", veiculos.get(0).getPlaca());
    }

    @Test
    void deveRetornarVazioQuandoNenhumVeiculoAtendeFiltro() {
        List<Veiculo> veiculos = service.listarVeiculos(new ListarVeiculosQuery(
                2020,
                "Honda",
                null,
                null,
                null,
                null));

        assertEquals(0, veiculos.size());
    }
}
