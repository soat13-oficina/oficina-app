package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestOrdemDeServicoRepository;
import br.com.oficina.support.persistence.TestVeiculoRepository;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

class CriarNovaOrdemDeServicoServiceTest {

    @Test
    void deveCriarNovaOrdemDeServico() {
        TestClienteRepository clienteRepository = new TestClienteRepository();
        TestVeiculoRepository veiculoRepository = new TestVeiculoRepository();
        TestOrdemDeServicoRepository ordemDeServicoRepository = new TestOrdemDeServicoRepository();
        UUID clienteId = UUID.fromString("31111111-1111-1111-1111-111111111111");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "11111111111", TipoCliente.PF));
        veiculoRepository.salvar(new Veiculo(
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
                ordemDeServicoRepository);

        service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand(clienteId.toString(), "func-1", "ABC1D23"));

        assertEquals(1, ordemDeServicoRepository.buscarTodas().size());
        assertEquals(clienteId, ordemDeServicoRepository.buscarTodas().get(0).getCliente().getId());
    }

    @Test
    void deveFalharQuandoClienteNaoExistir() {
        CriarNovaOrdemDeServicoService service = new CriarNovaOrdemDeServicoService(
                new TestClienteRepository(),
                new TestVeiculoRepository(),
                new TestOrdemDeServicoRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.criarNovaOrdemDeServico(new CriarOrdemDeServicoCommand(UUID.fromString("32222222-2222-2222-2222-222222222222").toString(), "func-1", "ABC1D23")));

        assertEquals("Cliente nao encontrado", exception.getMessage());
    }
}
