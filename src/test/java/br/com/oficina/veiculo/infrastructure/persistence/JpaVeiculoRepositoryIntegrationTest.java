package br.com.oficina.veiculo.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class JpaVeiculoRepositoryIntegrationTest {

    @Autowired
    private JpaVeiculoRepository repository;

    @Test
    void devePersistirBuscarPorFiltrosEBuscarTodos() {
        Veiculo corolla = new Veiculo(
                UUID.randomUUID(),
                "abc-1d23",
                "Toyota",
                "Corolla",
                "Toyota",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);
        Veiculo civic = new Veiculo(
                UUID.randomUUID(),
                "DEF2G34",
                "Honda",
                "Civic",
                "Honda",
                2023,
                155,
                "CVT",
                TipoCombustivel.GASOLINA);

        repository.salvar(corolla);
        repository.salvar(civic);

        Veiculo encontrado = repository.buscarPorPlaca(" ABC1D23 ").orElseThrow();
        assertEquals("ABC1D23", encontrado.getPlaca());
        assertEquals(2, repository.buscarTodos().size());

        List<Veiculo> filtrados = repository.buscarPorFiltros(
                "abc-1d23",
                2024,
                "Toyota",
                "Toyota",
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);

        assertEquals(1, filtrados.size());
        assertEquals("Corolla", filtrados.getFirst().getModelo());
    }

    @Test
    void deveExcluirVeiculoPorPlacaNormalizada() {
        String placa = "JKL3M45";
        repository.salvar(new Veiculo(
                UUID.randomUUID(),
                placa,
                "Volkswagen",
                "T-Cross",
                "Volkswagen",
                2022,
                150,
                "AUTOMATICO",
                TipoCombustivel.FLEX));

        assertTrue(repository.buscarPorPlaca(placa).isPresent());

        repository.excluirPorPlaca(" jkl-3m45 ");

        assertFalse(repository.buscarPorPlaca(placa).isPresent());
    }
}
