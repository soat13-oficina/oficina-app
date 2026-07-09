package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.command.ExcluirClienteCommand;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.ConflitoDeRecursoException;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.support.persistence.TestClienteRepository;

class ExcluirClienteServiceTest {

    private static final UUID CLIENTE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Test
    void deveExcluirClienteSemVinculos() {
        TestClienteRepository repository = new TestClienteRepository();
        repository.salvar(Cliente.reconstituir(CLIENTE_ID, "Maria", "12345678909", TipoCliente.PF));
        ExcluirClienteService service = new ExcluirClienteService(repository, clienteId -> false);

        service.excluirCliente(new ExcluirClienteCommand(CLIENTE_ID));

        assertTrue(repository.buscarPorId(CLIENTE_ID).isEmpty());
    }

    @Test
    void deveBloquearExclusaoQuandoClientePossuiVinculos() {
        TestClienteRepository repository = new TestClienteRepository();
        repository.salvar(Cliente.reconstituir(CLIENTE_ID, "Maria", "12345678909", TipoCliente.PF));
        ExcluirClienteService service = new ExcluirClienteService(repository, clienteId -> true);

        ConflitoDeRecursoException exception = assertThrows(
                ConflitoDeRecursoException.class,
                () -> service.excluirCliente(new ExcluirClienteCommand(CLIENTE_ID)));

        assertEquals("Nao e possivel excluir o cliente: existem veiculos ou ordens de servico vinculados.",
                exception.getMessage());
        assertTrue(repository.buscarPorId(CLIENTE_ID).isPresent());
    }

    @Test
    void deveFalharAoExcluirClienteInexistente() {
        ExcluirClienteService service = new ExcluirClienteService(new TestClienteRepository(), clienteId -> false);

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.excluirCliente(new ExcluirClienteCommand(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"))));

        assertEquals("Cliente nao encontrado para o identificador informado.", exception.getMessage());
    }
}
