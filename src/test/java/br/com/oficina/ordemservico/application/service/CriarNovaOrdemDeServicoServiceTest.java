package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestFuncionarioRepository;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.support.persistence.TestVeiculoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class CriarNovaOrdemDeServicoServiceTest {

    @Test
    void deveCriarNovaOrdemDeServico() {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestFuncionarioRepository funcionarioRepository = new TestFuncionarioRepository();
        TestVeiculoRepository veiculoRepository = new TestVeiculoRepository();
        TestOrdemDeServicoRepository ordemDeServicoRepository = new TestOrdemDeServicoRepository();
        UUID clienteId = UUID.fromString("31111111-1111-1111-1111-111111111111");
        UUID funcionarioId = UUID.fromString("41111111-1111-1111-1111-111111111111");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "11144477735", TipoCliente.PF));
        funcionarioRepository.salvar(Funcionario.reconstituir(funcionarioId, "Joao", "12345678909"));
        veiculoRepository.salvar(new Veiculo(
                clienteId,
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        CriarNovaOrdemDeServicoService service = new CriarNovaOrdemDeServicoService(
                clienteRepository,
                veiculoRepository,
                funcionarioRepository,
                ordemDeServicoRepository);

        service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand(clienteId.toString(), funcionarioId.toString(), "ABC1D23"));

        assertEquals(1, ordemDeServicoRepository.buscarTodas().size());
        assertEquals(clienteId, ordemDeServicoRepository.buscarTodas().get(0).getCliente().getId());
        assertEquals(funcionarioId, ordemDeServicoRepository.buscarTodas().get(0).getFuncionario().getId());
    }

    @Test
    void deveFalharQuandoClienteNaoExistir() {
        CriarNovaOrdemDeServicoService service = new CriarNovaOrdemDeServicoService(
                new TestClienteRepository(),
                new TestVeiculoRepository(),
                new TestFuncionarioRepository(),
                new TestOrdemDeServicoRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand(
                        UUID.fromString("32222222-2222-2222-2222-222222222222").toString(),
                        UUID.fromString("42222222-2222-2222-2222-222222222222").toString(),
                        "ABC1D23")));

        assertEquals("Cliente nao encontrado para o identificador informado.", exception.getMessage());
    }

    @Test
    void deveFalharQuandoFuncionarioIdForInvalido() {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestVeiculoRepository veiculoRepository = new TestVeiculoRepository();
        UUID clienteId = UUID.fromString("31111111-1111-1111-1111-111111111111");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "11144477735", TipoCliente.PF));
        veiculoRepository.salvar(new Veiculo(
                clienteId,
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        CriarNovaOrdemDeServicoService service = new CriarNovaOrdemDeServicoService(
                clienteRepository,
                veiculoRepository,
                new TestFuncionarioRepository(),
                new TestOrdemDeServicoRepository());

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand(
                        clienteId.toString(),
                        "funcionario-invalido",
                        "ABC1D23")));

        assertEquals("Identificador do funcionario invalido.", exception.getMessage());
    }
}
