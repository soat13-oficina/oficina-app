package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.command.ExcluirClienteCommand;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.support.persistence.TestClienteRepository;

class ExcluirClienteServiceTest {

    @Test
    void deveExcluirClienteExistente() {
        TestClienteRepository repository = new TestClienteRepository();
        UUID clienteId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        repository.salvar(Cliente.reconstituir(clienteId, "Maria", "12345678901", TipoCliente.PF));
        ExcluirClienteService service = new ExcluirClienteService(repository);

        service.excluirCliente(new ExcluirClienteCommand(clienteId));

        assertTrue(repository.buscarPorId(clienteId).isEmpty());
    }

    @Test
    void deveFalharAoExcluirClienteInexistente() {
        ExcluirClienteService service = new ExcluirClienteService(new TestClienteRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.excluirCliente(new ExcluirClienteCommand(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"))));

        assertEquals("Cliente nao encontrado", exception.getMessage());
    }
}
