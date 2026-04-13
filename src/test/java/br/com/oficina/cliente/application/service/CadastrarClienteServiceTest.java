package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.support.persistence.TestClienteRepository;

class CadastrarClienteServiceTest {

    @Test
    void deveCadastrarCliente() {
        TestClienteRepository repository = new TestClienteRepository();
        CadastrarClienteService service = new CadastrarClienteService(repository);

        String clienteId = service.cadastrarCliente(new CadastrarClienteCommand("Maria", "12345678901", TipoCliente.PF));

        assertEquals("Maria", repository.buscarPorId(clienteId).orElseThrow().getNome());
        assertEquals("12345678901", repository.buscarPorId(clienteId).orElseThrow().getCpfOuCnpj());
        assertEquals(TipoCliente.PF, repository.buscarPorId(clienteId).orElseThrow().getTipoCliente());
    }

    @Test
    void deveFalharAoCadastrarPessoaFisicaComCpfInvalido() {
        TestClienteRepository repository = new TestClienteRepository();
        CadastrarClienteService service = new CadastrarClienteService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarCliente(new CadastrarClienteCommand("Maria", "1234567890", TipoCliente.PF)));

        assertEquals("CPF deve possuir 11 digitos", exception.getMessage());
    }
}
