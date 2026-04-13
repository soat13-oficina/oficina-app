package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.query.ConsultarClienteQuery;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.support.persistence.TestClienteRepository;

class ConsultarClienteServiceTest {

    @Test
    void deveConsultarClientePorId() {
        TestClienteRepository repository = new TestClienteRepository();
        repository.salvar(new Cliente("cliente-1", "Maria", "12345678901", TipoCliente.PF));
        ConsultarClienteService service = new ConsultarClienteService(repository);

        Cliente cliente = service.consultarCliente(new ConsultarClienteQuery("cliente-1")).orElseThrow();

        assertEquals("Maria", cliente.getNome());
        assertEquals("12345678901", cliente.getCpfOuCnpj());
    }

    @Test
    void deveRetornarVazioQuandoClienteNaoExistir() {
        ConsultarClienteService service = new ConsultarClienteService(new TestClienteRepository());

        assertTrue(service.consultarCliente(new ConsultarClienteQuery("cliente-404")).isEmpty());
    }
}
