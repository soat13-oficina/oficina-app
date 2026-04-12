package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.command.ExcluirClienteCommand;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.infrastructure.persistence.InMemoryClienteRepository;

class ExcluirClienteServiceTest {

    @Test
    void deveExcluirClienteExistente() {
        InMemoryClienteRepository repository = new InMemoryClienteRepository();
        repository.salvar(new Cliente("cliente-1", "Maria", "12345678901"));
        ExcluirClienteService service = new ExcluirClienteService(repository);

        service.excluirCliente(new ExcluirClienteCommand("cliente-1"));

        assertTrue(repository.buscarPorId("cliente-1").isEmpty());
    }

    @Test
    void deveFalharAoExcluirClienteInexistente() {
        ExcluirClienteService service = new ExcluirClienteService(new InMemoryClienteRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.excluirCliente(new ExcluirClienteCommand("cliente-404")));

        assertEquals("Cliente nao encontrado", exception.getMessage());
    }
}
