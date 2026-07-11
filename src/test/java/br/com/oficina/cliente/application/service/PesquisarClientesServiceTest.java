package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.query.PesquisarClientesQuery;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.support.persistence.TestClienteRepository;

class PesquisarClientesServiceTest {

    @Test
    void devePesquisarClientesPorCpf() {
        TestClienteRepository repository = new TestClienteRepository();
        repository.salvar(Cliente.reconstituir(UUID.randomUUID(), "Maria Silva", "12345678909", TipoCliente.PF));
        repository.salvar(Cliente.reconstituir(UUID.randomUUID(), "Jose Souza", "20100000053", TipoCliente.PF));
        PesquisarClientesService service = new PesquisarClientesService(repository);

        List<Cliente> clientes = service.pesquisarClientes(new PesquisarClientesQuery("123.456.789-09"));

        assertEquals(1, clientes.size());
        assertEquals("Maria Silva", clientes.getFirst().getNome());
    }

    @Test
    void devePesquisarClientesPorPrimeiroNomeNomeCompletoOuSobrenome() {
        TestClienteRepository repository = new TestClienteRepository();
        repository.salvar(Cliente.reconstituir(UUID.randomUUID(), "Maria Silva", "12345678909", TipoCliente.PF));
        repository.salvar(Cliente.reconstituir(UUID.randomUUID(), "Joao Pereira", "20100000053", TipoCliente.PF));
        PesquisarClientesService service = new PesquisarClientesService(repository);

        List<Cliente> porPrimeiroNome = service.pesquisarClientes(new PesquisarClientesQuery("Maria"));
        List<Cliente> porNomeCompleto = service.pesquisarClientes(new PesquisarClientesQuery("Maria Silva"));
        List<Cliente> porSobrenome = service.pesquisarClientes(new PesquisarClientesQuery("Pereira"));

        assertEquals(1, porPrimeiroNome.size());
        assertEquals("Maria Silva", porPrimeiroNome.getFirst().getNome());
        assertEquals(1, porNomeCompleto.size());
        assertEquals("Maria Silva", porNomeCompleto.getFirst().getNome());
        assertEquals(1, porSobrenome.size());
        assertEquals("Joao Pereira", porSobrenome.getFirst().getNome());
    }
}
