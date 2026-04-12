package br.com.oficina.veiculo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.infrastructure.persistence.InMemoryVeiculoRepository;

class CadastrarVeiculoServiceTest {

    @Test
    void deveCadastrarVeiculoNoRepositorio() {
        InMemoryVeiculoRepository repository = new InMemoryVeiculoRepository();
        CadastrarVeiculoService service = new CadastrarVeiculoService(repository);

        service.cadastrarVeiculo(new CadastrarVeiculoCommand(
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX,
                "cliente-1"));

        assertEquals("Corolla", repository.buscarPorPlaca("ABC1D23").orElseThrow().getModelo());
    }
}
