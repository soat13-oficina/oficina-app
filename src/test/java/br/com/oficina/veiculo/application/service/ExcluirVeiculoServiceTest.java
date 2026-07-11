package br.com.oficina.veiculo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.support.persistence.TestVeiculoRepository;
import br.com.oficina.veiculo.application.command.ExcluirVeiculoCommand;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

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
        ExcluirVeiculoService service = new ExcluirVeiculoService(repository, new TestOrdemDeServicoRepository());

        service.excluirVeiculo(new ExcluirVeiculoCommand("abc-1d23"));

        assertTrue(repository.buscarPorPlaca("ABC1D23").isEmpty());
    }

    @Test
    void deveFalharAoExcluirVeiculoInexistente() {
        TestVeiculoRepository repository = new TestVeiculoRepository();
        ExcluirVeiculoService service = new ExcluirVeiculoService(repository, new TestOrdemDeServicoRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.excluirVeiculo(new ExcluirVeiculoCommand("ABC1D23")));

        assertEquals("Veiculo nao encontrado para a placa informada.", exception.getMessage());
    }

    @Test
    void deveFalharAoExcluirVeiculoVinculadoAOrdemDeServico() {
        TestVeiculoRepository veiculoRepository = new TestVeiculoRepository();
        TestOrdemDeServicoRepository ordemDeServicoRepository = new TestOrdemDeServicoRepository();

        UUID veiculoId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID clienteId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        Veiculo veiculo = Veiculo.reconstituir(veiculoId, clienteId, "ABC1D23",
                "Ford", "Ranger", "Ford Motor Company", 2022, 213, "AUTOMATICO", TipoCombustivel.DIESEL);
        veiculoRepository.salvar(veiculo);

        Cliente cliente = Cliente.reconstituir(clienteId, "Cliente Teste", "12345678909", TipoCliente.PF);
        Funcionario funcionario = Funcionario.reconstituir(UUID.randomUUID(), "Mecanico", null);
        ordemDeServicoRepository.salvar(
                OrdemDeServico.abrir(UUID.randomUUID(), "OS-VEIC01", funcionario, cliente, veiculo));

        ExcluirVeiculoService service = new ExcluirVeiculoService(veiculoRepository, ordemDeServicoRepository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.excluirVeiculo(new ExcluirVeiculoCommand("ABC1D23")));

        assertEquals("Nao e possivel excluir o veiculo: existem ordens de servico vinculadas.", exception.getMessage());
        assertTrue(veiculoRepository.buscarPorPlaca("ABC1D23").isPresent());
    }
}
