package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.query.ConsultarClienteQuery;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.support.persistence.TestClienteRepository;

class ConsultarClienteServiceTest {

    @Test
    void deveConsultarClientePorId() {
        TestClienteRepository repository = new TestClienteRepository();
        UUID clienteId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        repository.salvar(Cliente.reconstituir(clienteId, "Maria", "12345678909", TipoCliente.PF));
        ConsultarClienteService service = new ConsultarClienteService(repository);

        Cliente cliente = service.consultarCliente(new ConsultarClienteQuery(clienteId)).orElseThrow();

        assertEquals("Maria", cliente.getNome());
        assertEquals("12345678909", cliente.getCpfOuCnpj());
    }

    @Test
    void deveRetornarVazioQuandoClienteNaoExistir() {
        ConsultarClienteService service = new ConsultarClienteService(new TestClienteRepository());

        assertTrue(service.consultarCliente(new ConsultarClienteQuery(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))).isEmpty());
    }
}
