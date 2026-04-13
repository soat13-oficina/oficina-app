package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.command.AlterarClienteCommand;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.support.persistence.TestClienteRepository;

class AlterarClienteServiceTest {

    @Test
    void deveAlterarClienteExistente() {
        TestClienteRepository repository = new TestClienteRepository();
        repository.salvar(new Cliente("cliente-1", "Maria", "12345678901", TipoCliente.PF));
        AlterarClienteService service = new AlterarClienteService(repository);

        service.alterarCliente(new AlterarClienteCommand("cliente-1", "Bianca", "11222333000199", TipoCliente.PJ));

        Cliente clienteAtualizado = repository.buscarPorId("cliente-1").orElseThrow();
        assertEquals("Bianca", clienteAtualizado.getNome());
        assertEquals("11222333000199", clienteAtualizado.getCpfOuCnpj());
        assertEquals(TipoCliente.PJ, clienteAtualizado.getTipoCliente());
    }

    @Test
    void deveFalharAoAlterarClienteInexistente() {
        AlterarClienteService service = new AlterarClienteService(new TestClienteRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.alterarCliente(new AlterarClienteCommand("cliente-404", "Bianca", "99999999999", TipoCliente.PF)));

        assertEquals("Cliente nao encontrado", exception.getMessage());
    }

    @Test
    void deveFalharAoAlterarClienteParaCnpjInvalido() {
        TestClienteRepository repository = new TestClienteRepository();
        repository.salvar(new Cliente("cliente-1", "Maria", "12345678901", TipoCliente.PF));
        AlterarClienteService service = new AlterarClienteService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.alterarCliente(new AlterarClienteCommand("cliente-1", "Empresa", "1234567800019", TipoCliente.PJ)));

        assertEquals("CNPJ deve possuir 14 digitos", exception.getMessage());
    }
}
