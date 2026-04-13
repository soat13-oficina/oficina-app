package br.com.oficina.veiculo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.oficina.support.persistence.TestVeiculoRepository;
import br.com.oficina.veiculo.application.query.ConsultarVeiculosQuery;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class ConsultarVeiculosServiceTest {
    private TestVeiculoRepository repository;
    private ConsultarVeiculosService service;

    @BeforeEach
    void setUp() {
        repository = new TestVeiculoRepository();
        service = new ConsultarVeiculosService(repository);
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
    void deveConsultarTodosQuandoNaoHaFiltros() {
        List<Veiculo> veiculos = service.consultarVeiculos(
                new ConsultarVeiculosQuery(null, null, null, null, null, null, null));

        assertEquals(3, veiculos.size());
    }

    @Test
    void deveConsultarPorPlacaMarcaFabricanteAnoPotenciaCambioETipo() {
        List<Veiculo> veiculos = service.consultarVeiculos(new ConsultarVeiculosQuery(
                "ABC1D23",
                2024,
                "Toyota",
                "Toyota Motor Corporation",
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX));

        assertEquals(1, veiculos.size());
        assertEquals("ABC1D23", veiculos.getFirst().getPlaca());
    }

    @Test
    void deveConsultarPorPlacaOuMarcaOuFabricante() {
        List<Veiculo> porPlaca = service.consultarVeiculos(
                new ConsultarVeiculosQuery("TES1A23", null, null, null, null, null, null));
        List<Veiculo> porMarca = service.consultarVeiculos(
                new ConsultarVeiculosQuery(null, null, "Tesla", null, null, null, null));
        List<Veiculo> porFabricante = service.consultarVeiculos(
                new ConsultarVeiculosQuery(null, null, null, "Ford Motor Company", null, null, null));

        assertEquals(1, porPlaca.size());
        assertEquals("TES1A23", porPlaca.getFirst().getPlaca());
        assertEquals(1, porMarca.size());
        assertEquals("TES1A23", porMarca.getFirst().getPlaca());
        assertEquals(1, porFabricante.size());
        assertEquals("FOR1D23", porFabricante.getFirst().getPlaca());
    }

    @Test
    void deveRetornarVazioQuandoNenhumVeiculoAtendeConsulta() {
        List<Veiculo> veiculos = service.consultarVeiculos(new ConsultarVeiculosQuery(
                "ZZZ9Z99",
                2020,
                "Honda",
                null,
                null,
                null,
                null));

        assertEquals(0, veiculos.size());
    }
}
