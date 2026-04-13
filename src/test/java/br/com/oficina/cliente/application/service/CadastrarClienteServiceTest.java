package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;
import br.com.oficina.support.persistence.TestClienteRepository;

class CadastrarClienteServiceTest {

    @Test
    void deveCadastrarCliente() {
        TestClienteRepository repository = new TestClienteRepository();
        CadastrarClienteService service = new CadastrarClienteService(repository);

        service.cadastrarCliente(new CadastrarClienteCommand("cliente-1", "Maria", "12345678901"));

        assertEquals("Maria", repository.buscarPorId("cliente-1").orElseThrow().getNome());
        assertEquals("12345678901", repository.buscarPorId("cliente-1").orElseThrow().getCpf());
    }
}
