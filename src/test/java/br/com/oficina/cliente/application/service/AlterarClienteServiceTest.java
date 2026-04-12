package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.command.AlterarClienteCommand;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.infrastructure.persistence.InMemoryClienteRepository;

class AlterarClienteServiceTest {

    @Test
    void deveAlterarClienteExistente() {
        InMemoryClienteRepository repository = new InMemoryClienteRepository();
        repository.salvar(new Cliente("cliente-1", "Maria", "12345678901"));
        AlterarClienteService service = new AlterarClienteService(repository);

        service.alterarCliente(new AlterarClienteCommand("cliente-1", "Bianca", "99999999999"));

        Cliente clienteAtualizado = repository.buscarPorId("cliente-1").orElseThrow();
        assertEquals("Bianca", clienteAtualizado.getNome());
        assertEquals("99999999999", clienteAtualizado.getCpf());
    }

    @Test
    void deveFalharAoAlterarClienteInexistente() {
        AlterarClienteService service = new AlterarClienteService(new InMemoryClienteRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarCliente(new AlterarClienteCommand("cliente-404", "Bianca", "99999999999")));

        assertEquals("Cliente nao encontrado", exception.getMessage());
    }
}
