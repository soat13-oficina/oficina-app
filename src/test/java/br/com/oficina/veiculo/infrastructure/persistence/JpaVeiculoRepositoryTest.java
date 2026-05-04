package br.com.oficina.veiculo.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class JpaVeiculoRepositoryTest {

    @Mock
    private SpringDataVeiculoRepository springDataVeiculoRepository;

    private JpaVeiculoRepository jpaVeiculoRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jpaVeiculoRepository = new JpaVeiculoRepository(springDataVeiculoRepository);
    }

    @Test
    void deveSalvarVeiculoDiretamenteNoRepositorioSpringData() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);

        jpaVeiculoRepository.salvar(veiculo);

        verify(springDataVeiculoRepository).save(veiculo);
    }

    @Test
    void deveBuscarVeiculoPorPlacaNormalizada() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);
        when(springDataVeiculoRepository.findByPlaca("ABC1D23")).thenReturn(Optional.of(veiculo));

        Optional<Veiculo> encontrado = jpaVeiculoRepository.buscarPorPlaca(" abc-1d23 ");

        assertTrue(encontrado.isPresent());
        assertSame(veiculo, encontrado.orElseThrow());
    }

    @Test
    void deveBuscarTodosOsVeiculos() {
        Veiculo veiculo = new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);
        when(springDataVeiculoRepository.findAll()).thenReturn(List.of(veiculo));

        List<Veiculo> veiculos = jpaVeiculoRepository.buscarTodos();

        assertEquals(1, veiculos.size());
        assertSame(veiculo, veiculos.getFirst());
    }

    @Test
    void deveExcluirVeiculoPorPlacaNormalizada() {
        jpaVeiculoRepository.excluirPorPlaca(" abc-1d23 ");

        verify(springDataVeiculoRepository).deleteByPlaca("ABC1D23");
    }
}
