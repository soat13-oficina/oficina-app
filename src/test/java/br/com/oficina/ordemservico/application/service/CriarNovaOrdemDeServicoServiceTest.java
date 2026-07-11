package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand.PecaItem;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand.ServicoItem;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestFuncionarioRepository;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;
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
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "20110101103", TipoCliente.PF));
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
                ordemDeServicoRepository,
                new TestPecaInsumoRepository());

        String numeroOrdemServico = service.criarNovaOrdemDeServico(
                new CriarOrdemDeServicoCommand(clienteId.toString(), funcionarioId.toString(), "ABC1D23"));

        assertNotNull(numeroOrdemServico);
        assertEquals(1, ordemDeServicoRepository.buscarTodas().size());
        assertEquals(StatusOrdemDeServico.OS_ABERTA, ordemDeServicoRepository.buscarTodas().get(0).getStatus());
        assertEquals(clienteId, ordemDeServicoRepository.buscarTodas().get(0).getCliente().getId());
        assertEquals(funcionarioId, ordemDeServicoRepository.buscarTodas().get(0).getFuncionario().getId());
    }

    @Test
    void deveCriarOrdemComServicosEPecasValidos() {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestFuncionarioRepository funcionarioRepository = new TestFuncionarioRepository();
        TestVeiculoRepository veiculoRepository = new TestVeiculoRepository();
        TestOrdemDeServicoRepository ordemDeServicoRepository = new TestOrdemDeServicoRepository();
        TestPecaInsumoRepository pecaInsumoRepository = new TestPecaInsumoRepository();
        UUID clienteId = UUID.fromString("31111111-1111-1111-1111-111111111111");
        UUID funcionarioId = UUID.fromString("41111111-1111-1111-1111-111111111111");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "20110101103", TipoCliente.PF));
        funcionarioRepository.salvar(Funcionario.reconstituir(funcionarioId, "Joao", "12345678909"));
        veiculoRepository.salvar(new Veiculo(
                clienteId, "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO",
                TipoCombustivel.FLEX));
        pecaInsumoRepository.salvar(new PecaInsumo(
                "PECA-1", "Pastilha", "Bosch", new BigDecimal("250.00"), 10, 0, "REF-001", CategoriaPeca.FREIOS));
        CriarNovaOrdemDeServicoService service = new CriarNovaOrdemDeServicoService(
                clienteRepository, veiculoRepository, funcionarioRepository, ordemDeServicoRepository, pecaInsumoRepository);

        String numeroOrdemServico = service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand(
                clienteId.toString(),
                funcionarioId.toString(),
                "ABC1D23",
                List.of(new ServicoItem("Troca de pastilhas", new BigDecimal("150.00"))),
                List.of(new PecaItem("PECA-1", 2))));

        assertNotNull(numeroOrdemServico);
        assertEquals(1, ordemDeServicoRepository.buscarTodas().size());
    }

    @Test
    void deveFalharQuandoPecaPrevistaNaoExistir() {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestFuncionarioRepository funcionarioRepository = new TestFuncionarioRepository();
        TestVeiculoRepository veiculoRepository = new TestVeiculoRepository();
        UUID clienteId = UUID.fromString("31111111-1111-1111-1111-111111111111");
        UUID funcionarioId = UUID.fromString("41111111-1111-1111-1111-111111111111");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "20110101103", TipoCliente.PF));
        funcionarioRepository.salvar(Funcionario.reconstituir(funcionarioId, "Joao", "12345678909"));
        veiculoRepository.salvar(new Veiculo(
                clienteId, "ABC1D23", "Toyota", "Corolla", "Toyota Motor Corporation", 2024, 177, "AUTOMATICO",
                TipoCombustivel.FLEX));
        CriarNovaOrdemDeServicoService service = new CriarNovaOrdemDeServicoService(
                clienteRepository, veiculoRepository, funcionarioRepository, new TestOrdemDeServicoRepository(),
                new TestPecaInsumoRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand(
                        clienteId.toString(),
                        funcionarioId.toString(),
                        "ABC1D23",
                        List.of(),
                        List.of(new PecaItem("PECA-INEXISTENTE", 1)))));

        assertEquals("Peca nao encontrada para o identificador informado: PECA-INEXISTENTE", exception.getMessage());
    }

    @Test
    void deveFalharQuandoClienteNaoExistir() {
        CriarNovaOrdemDeServicoService service = new CriarNovaOrdemDeServicoService(
                new TestClienteRepository(),
                new TestVeiculoRepository(),
                new TestFuncionarioRepository(),
                new TestOrdemDeServicoRepository(),
                new TestPecaInsumoRepository());

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
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "20110101103", TipoCliente.PF));
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
                new TestOrdemDeServicoRepository(),
                new TestPecaInsumoRepository());

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand(
                        clienteId.toString(),
                        "funcionario-invalido",
                        "ABC1D23")));

        assertEquals("Identificador do funcionario invalido.", exception.getMessage());
    }
}
