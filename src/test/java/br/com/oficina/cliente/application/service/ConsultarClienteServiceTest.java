package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.query.ConsultarClienteQuery;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.infrastructure.persistence.InMemoryClienteRepository;

class ConsultarClienteServiceTest {

    @Test
    void deveConsultarClientePorId() {
        InMemoryClienteRepository repository = new InMemoryClienteRepository();
        repository.salvar(new Cliente("cliente-1", "Maria", "12345678901"));
        ConsultarClienteService service = new ConsultarClienteService(repository);

        Cliente cliente = service.consultarCliente(new ConsultarClienteQuery("cliente-1")).orElseThrow();

        assertEquals("Maria", cliente.getNome());
    }

    @Test
    void deveRetornarVazioQuandoClienteNaoExistir() {
        ConsultarClienteService service = new ConsultarClienteService(new InMemoryClienteRepository());

        assertTrue(service.consultarCliente(new ConsultarClienteQuery("cliente-404")).isEmpty());
    }
}
