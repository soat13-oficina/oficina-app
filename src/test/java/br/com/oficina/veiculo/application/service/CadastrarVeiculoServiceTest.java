package br.com.oficina.veiculo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.support.persistence.TestClienteRepository;
import br.com.oficina.support.persistence.TestVeiculoRepository;

class CadastrarVeiculoServiceTest {

    @Test
    void deveCadastrarVeiculoNoRepositorio() {
        TestVeiculoRepository repository = new TestVeiculoRepository();
        TestClienteRepository clienteRepository = new TestClienteRepository();
        UUID clienteId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        clienteRepository.salvar(Cliente.reconstituir(clienteId, "Maria", "12345678909", TipoCliente.PF));
        CadastrarVeiculoService service = new CadastrarVeiculoService(repository, clienteRepository);

        service.cadastrarVeiculo(new CadastrarVeiculoCommand(
                "abc-1d23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX,
                clienteId.toString()));

        assertEquals("Corolla", repository.buscarPorPlaca("ABC1D23").orElseThrow().getModelo());
        assertEquals("ABC1D23", repository.buscarPorPlaca("ABC1D23").orElseThrow().getPlaca());
        assertEquals(clienteId, repository.buscarPorPlaca("ABC1D23").orElseThrow().getClienteId());
    }

    @Test
    void deveFalharAoCadastrarVeiculoComPlacaDuplicada() {
        TestVeiculoRepository repository = new TestVeiculoRepository();
        TestClienteRepository clienteRepository = new TestClienteRepository();
        UUID clienteId1 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID clienteId2 = UUID.fromString("33333333-3333-3333-3333-333333333333");
        clienteRepository.salvar(Cliente.reconstituir(clienteId1, "Maria", "12345678909", TipoCliente.PF));
        clienteRepository.salvar(Cliente.reconstituir(clienteId2, "Joao", "52998224725", TipoCliente.PF));
        repository.salvar(new Veiculo(
                clienteId1,
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Toyota Motor Corporation",
                2024,
                177,
                "AUTOMATICO",
                TipoCombustivel.FLEX));
        CadastrarVeiculoService service = new CadastrarVeiculoService(repository, clienteRepository);

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
                        clienteId2.toString())));

        assertEquals("Ja existe veiculo cadastrado com a mesma placa.", exception.getMessage());
    }
}
