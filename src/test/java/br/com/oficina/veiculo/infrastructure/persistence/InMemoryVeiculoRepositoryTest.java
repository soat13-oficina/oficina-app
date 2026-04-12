package br.com.oficina.veiculo.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class InMemoryVeiculoRepositoryTest {

    @Test
    void deveSalvarBuscarListarEExcluirVeiculo() {
        InMemoryVeiculoRepository repository = new InMemoryVeiculoRepository();
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);

        repository.salvar(veiculo);

        assertEquals("Corolla", repository.buscarPorPlaca("ABC1D23").orElseThrow().getModelo());
        assertEquals(1, repository.buscarTodos().size());

        repository.excluirPorPlaca("ABC1D23");

        assertTrue(repository.buscarPorPlaca("ABC1D23").isEmpty());
        assertEquals(0, repository.buscarTodos().size());
    }
}
