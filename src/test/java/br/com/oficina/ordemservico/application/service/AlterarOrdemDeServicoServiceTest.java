package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.application.command.AlterarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.support.persistence.TestVeiculoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class AlterarOrdemDeServicoServiceTest {

    @Test
    void deveAlterarOrdemDeServicoExistente() {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestVeiculoRepository veiculoRepository = new TestVeiculoRepository();
        TestOrdemDeServicoRepository ordemDeServicoRepository = new TestOrdemDeServicoRepository();
        UUID clienteId1 = UUID.fromString("21111111-1111-1111-1111-111111111111");
        UUID clienteId2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        clienteRepository.salvar(Cliente.reconstituir(clienteId1, "Maria", "11111111111", TipoCliente.PF));
        clienteRepository.salvar(Cliente.reconstituir(clienteId2, "Bianca", "22222222222", TipoCliente.PF));
        veiculoRepository.salvar(novoVeiculo("ABC1D23", "Toyota"));
        veiculoRepository.salvar(novoVeiculo("XYZ9Z99", "Honda"));
        ordemDeServicoRepository.salvar(OrdemDeServico.abrir(
                "id-1",
                "OS-001",
                new Funcionario("func-1", "Joao", null),
                Cliente.reconstituir(clienteId1, "Maria", "11111111111", TipoCliente.PF),
                novoVeiculo("ABC1D23", "Toyota")));
        AlterarOrdemDeServicoService service = new AlterarOrdemDeServicoService(
                clienteRepository,
                veiculoRepository,
                ordemDeServicoRepository);

        service.alterarOrdemDeServico(new AlterarOrdemDeServicoCommand("OS-001", clienteId2.toString(), "func-2", "XYZ9Z99"));

        OrdemDeServico alterada = ordemDeServicoRepository.buscarPorNumero("OS-001").orElseThrow();
        assertEquals(clienteId2, alterada.getCliente().getId());
        assertEquals("XYZ9Z99", alterada.getVeiculo().getPlaca());
        assertEquals("func-2", alterada.getFuncionario().getId());
    }

    @Test
    void deveFalharQuandoOrdemNaoExistir() {
        AlterarOrdemDeServicoService service = new AlterarOrdemDeServicoService(
                new TestClienteRepository(),
                new TestVeiculoRepository(),
                new TestOrdemDeServicoRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarOrdemDeServico(new AlterarOrdemDeServicoCommand("OS-404", UUID.fromString("23333333-3333-3333-3333-333333333333").toString(), "func-1", "ABC1D23")));

        assertEquals("Ordem de servico nao encontrada", exception.getMessage());
    }

    private Veiculo novoVeiculo(String placa, String marca) {
        return new Veiculo(
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
