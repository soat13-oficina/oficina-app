package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.application.command.AlterarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestFuncionarioRepository;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.support.persistence.TestVeiculoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class AlterarOrdemDeServicoServiceTest {

    @Test
    void deveAlterarOrdemDeServicoExistente() {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestFuncionarioRepository funcionarioRepository = new TestFuncionarioRepository();
        TestVeiculoRepository veiculoRepository = new TestVeiculoRepository();
        TestOrdemDeServicoRepository ordemDeServicoRepository = new TestOrdemDeServicoRepository();
        UUID clienteId1 = UUID.fromString("21111111-1111-1111-1111-111111111111");
        UUID clienteId2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID funcionarioId = UUID.fromString("31111111-1111-1111-1111-111111111111");
        clienteRepository.salvar(Cliente.reconstituir(clienteId1, "Maria", "11111111111", TipoCliente.PF));
        clienteRepository.salvar(Cliente.reconstituir(clienteId2, "Bianca", "22222222222", TipoCliente.PF));
        funcionarioRepository.salvar(Funcionario.reconstituir(funcionarioId, "Joao", null));
        veiculoRepository.salvar(novoVeiculo(clienteId1, "ABC1D23", "Toyota"));
        veiculoRepository.salvar(novoVeiculo(clienteId2, "XYZ9Z99", "Honda"));
        Veiculo veiculoOriginal = veiculoRepository.buscarPorPlaca("ABC1D23").orElseThrow();
        ordemDeServicoRepository.salvar(OrdemDeServico.abrir(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "OS-001",
                Funcionario.reconstituir(funcionarioId, "Joao", null),
                Cliente.reconstituir(clienteId1, "Maria", "11111111111", TipoCliente.PF),
                veiculoOriginal));
        AlterarOrdemDeServicoService service = new AlterarOrdemDeServicoService(
                clienteRepository,
                veiculoRepository,
                funcionarioRepository,
                ordemDeServicoRepository);

        service.alterarOrdemDeServico(new AlterarOrdemDeServicoCommand("OS-001", clienteId2.toString(), funcionarioId.toString(), "XYZ9Z99"));

        OrdemDeServico alterada = ordemDeServicoRepository.buscarPorNumero("OS-001").orElseThrow();
        assertEquals(clienteId2, alterada.getCliente().getId());
        assertEquals("XYZ9Z99", alterada.getVeiculo().getPlaca());
        assertEquals(funcionarioId, alterada.getFuncionario().getId());
        assertNull(alterada.getIniciadaEm());
    }

    @Test
    void deveFalharQuandoTentarAlterarFuncionarioCriador() {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestFuncionarioRepository funcionarioRepository = new TestFuncionarioRepository();
        TestVeiculoRepository veiculoRepository = new TestVeiculoRepository();
        TestOrdemDeServicoRepository ordemDeServicoRepository = new TestOrdemDeServicoRepository();
        UUID clienteId = UUID.fromString("24444444-4444-4444-4444-444444444444");
        UUID funcionarioCriadorId = UUID.fromString("34444444-4444-4444-4444-444444444444");
        UUID outroFuncionarioId = UUID.fromString("35555555-5555-5555-5555-555555555555");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "11111111111", TipoCliente.PF));
        funcionarioRepository.salvar(Funcionario.reconstituir(funcionarioCriadorId, "Joao", null));
        funcionarioRepository.salvar(Funcionario.reconstituir(outroFuncionarioId, "Carlos", null));
        veiculoRepository.salvar(novoVeiculo(clienteId, "ABC1D23", "Toyota"));
        ordemDeServicoRepository.salvar(OrdemDeServico.abrir(
                UUID.fromString("12222222-2222-2222-2222-222222222222"),
                "OS-002",
                Funcionario.reconstituir(funcionarioCriadorId, "Joao", null),
                Cliente.reconstituir(clienteId, "Maria", "11111111111", TipoCliente.PF),
                veiculoRepository.buscarPorPlaca("ABC1D23").orElseThrow()));
        AlterarOrdemDeServicoService service = new AlterarOrdemDeServicoService(
                clienteRepository,
                veiculoRepository,
                funcionarioRepository,
                ordemDeServicoRepository);

        var exception = assertThrows(
                br.com.oficina.common.domain.exception.RegraDeNegocioException.class,
                () -> service.alterarOrdemDeServico(new AlterarOrdemDeServicoCommand("OS-002", clienteId.toString(), outroFuncionarioId.toString(), "ABC1D23")));

        assertEquals("Funcionario criador da ordem de servico nao pode ser alterado.", exception.getMessage());
    }

    @Test
    void deveFalharQuandoOrdemNaoExistir() {
        AlterarOrdemDeServicoService service = new AlterarOrdemDeServicoService(
                new TestClienteRepository(),
                new TestVeiculoRepository(),
                new TestFuncionarioRepository(),
                new TestOrdemDeServicoRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarOrdemDeServico(new AlterarOrdemDeServicoCommand(
                        "OS-404",
                        UUID.fromString("23333333-3333-3333-3333-333333333333").toString(),
                        UUID.fromString("43333333-3333-3333-3333-333333333333").toString(),
                        "ABC1D23")));

        assertEquals("Ordem de servico nao encontrada", exception.getMessage());
    }

    private Veiculo novoVeiculo(UUID clienteId, String placa, String marca) {
        return new Veiculo(
                clienteId,
                placa,
                marca,
                "Modelo",
                "Fabricante",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX);
    }
}
