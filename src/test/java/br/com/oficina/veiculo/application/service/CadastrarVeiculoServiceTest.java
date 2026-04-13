package br.com.oficina.veiculo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.support.persistence.TestVeiculoRepository;

class CadastrarVeiculoServiceTest {

    @Test
    void deveCadastrarVeiculoNoRepositorio() {
        TestVeiculoRepository repository = new TestVeiculoRepository();
        CadastrarVeiculoService service = new CadastrarVeiculoService(repository);

        service.cadastrarVeiculo(new CadastrarVeiculoCommand(
                "abc-1d23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX,
                "cliente-1"));

        assertEquals("Corolla", repository.buscarPorPlaca("ABC1D23").orElseThrow().getModelo());
        assertEquals("ABC1D23", repository.buscarPorPlaca("ABC1D23").orElseThrow().getPlaca());
    }

    @Test
    void deveFalharAoCadastrarVeiculoComPlacaDuplicada() {
        TestVeiculoRepository repository = new TestVeiculoRepository();
        repository.salvar(new Veiculo(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        CadastrarVeiculoService service = new CadastrarVeiculoService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarVeiculo(new CadastrarVeiculoCommand(
                        "abc-1d23",
                        "Honda",
                        "City",
                        "Honda",
                        2024,
                        126,
                        "AUTOMATICO",
                        TipoCombustivel.FLEX,
                        "cliente-2")));

        assertEquals("Ja existe veiculo cadastrado com a mesma placa.", exception.getMessage());
    }
}
