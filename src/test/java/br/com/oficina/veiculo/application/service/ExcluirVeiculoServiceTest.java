package br.com.oficina.veiculo.application.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.support.persistence.TestVeiculoRepository;

class ExcluirVeiculoServiceTest {

    @Test
    void deveExcluirVeiculoPorPlaca() {
        TestVeiculoRepository repository = new TestVeiculoRepository();
        repository.salvar(new Veiculo(
                "ABC1D23",
                "Ford",
                "Ranger",
                "Ford Motor Company",
                2022,
                213,
                "AUTOMATICO",
                TipoCombustivel.DIESEL));
        ExcluirVeiculoService service = new ExcluirVeiculoService(repository);

        service.excluirVeiculo(new ExcluirVeiculoCommand("ABC1D23"));

        assertTrue(repository.buscarPorPlaca("ABC1D23").isEmpty());
    }
}
