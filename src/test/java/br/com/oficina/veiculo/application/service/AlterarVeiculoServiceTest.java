package br.com.oficina.veiculo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.veiculo.application.command.AlterarVeiculoCommand;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.support.persistence.TestVeiculoRepository;

class AlterarVeiculoServiceTest {

    @Test
    void deveAlterarVeiculoExistente() {
        TestVeiculoRepository repository = new TestVeiculoRepository();
        repository.salvar(new Veiculo(
                "ABC1D23",
                "Volkswagen",
                "T-Cross",
                "Volkswagen AG",
                2023,
                128,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        AlterarVeiculoService service = new AlterarVeiculoService(repository);

        service.alterarVeiculo(new AlterarVeiculoCommand(
                "ABC1D23",
                "Volkswagen",
                "Taos",
                "Volkswagen AG",
                2024,
                150,
                "AUTOMATICO",
                TipoCombustivel.GASOLINA));

        Veiculo alterado = repository.buscarPorPlaca("ABC1D23").orElseThrow();
        assertEquals("Taos", alterado.getModelo());
        assertEquals(2024, alterado.getAno());
        assertEquals(TipoCombustivel.GASOLINA, alterado.getTipo());
    }

    @Test
    void deveFalharAoAlterarVeiculoInexistente() {
        TestVeiculoRepository repository = new TestVeiculoRepository();
        AlterarVeiculoService service = new AlterarVeiculoService(repository);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarVeiculo(new AlterarVeiculoCommand(
                        "ABC1D23",
                        "Volkswagen",
                        "Taos",
                        "Volkswagen AG",
                        2024,
                        150,
                        "AUTOMATICO",
                        TipoCombustivel.GASOLINA)));

        assertEquals("Veiculo nao encontrado", exception.getMessage());
    }
}
