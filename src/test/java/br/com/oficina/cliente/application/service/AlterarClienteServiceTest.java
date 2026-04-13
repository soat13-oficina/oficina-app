package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.command.AlterarClienteCommand;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.support.persistence.TestClienteRepository;

class AlterarClienteServiceTest {

    @Test
    void deveAlterarClienteExistente() {
        TestClienteRepository repository = new TestClienteRepository();
        UUID clienteId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        repository.salvar(Cliente.reconstituir(clienteId, "Maria", "12345678901", TipoCliente.PF));
        AlterarClienteService service = new AlterarClienteService(repository);

        service.alterarCliente(new AlterarClienteCommand(clienteId, "Bianca", "11222333000199", TipoCliente.PJ));

        Cliente clienteAtualizado = repository.buscarPorId(clienteId).orElseThrow();
        assertEquals("Bianca", clienteAtualizado.getNome());
        assertEquals("11222333000199", clienteAtualizado.getCpfOuCnpj());
        assertEquals(TipoCliente.PJ, clienteAtualizado.getTipoCliente());
    }

    @Test
    void deveFalharAoAlterarClienteInexistente() {
        AlterarClienteService service = new AlterarClienteService(new TestClienteRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.alterarCliente(new AlterarClienteCommand(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), "Bianca", "99999999999", TipoCliente.PF)));

        assertEquals("Cliente nao encontrado para o identificador informado.", exception.getMessage());
    }

    @Test
    void deveFalharAoAlterarClienteParaCnpjInvalido() {
        TestClienteRepository repository = new TestClienteRepository();
        UUID clienteId = UUID.fromString("12121212-1212-1212-1212-121212121212");
        repository.salvar(Cliente.reconstituir(clienteId, "Maria", "12345678901", TipoCliente.PF));
        AlterarClienteService service = new AlterarClienteService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.alterarCliente(new AlterarClienteCommand(clienteId, "Empresa", "1234567800019", TipoCliente.PJ)));

        assertEquals("CNPJ deve possuir 14 digitos", exception.getMessage());
    }
}
