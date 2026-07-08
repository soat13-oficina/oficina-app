package br.com.oficina.cliente.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.support.persistence.TestClienteRepository;

class CadastrarClienteServiceTest {

    @Test
    void deveCadastrarCliente() {
        TestClienteRepository repository = new TestClienteRepository();
        CadastrarClienteService service = new CadastrarClienteService(repository);

        var clienteId = service.cadastrarCliente(new CadastrarClienteCommand("Maria", "12345678909", TipoCliente.PF));

        assertNotNull(clienteId);
        assertEquals("Maria", repository.buscarPorId(clienteId).orElseThrow().getNome());
        assertEquals("12345678909", repository.buscarPorId(clienteId).orElseThrow().getCpfOuCnpj());
        assertEquals(TipoCliente.PF, repository.buscarPorId(clienteId).orElseThrow().getTipoCliente());
    }

    @Test
    void deveFalharAoCadastrarPessoaFisicaComCpfInvalido() {
        TestClienteRepository repository = new TestClienteRepository();
        CadastrarClienteService service = new CadastrarClienteService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarCliente(new CadastrarClienteCommand("Maria", "1234567890", TipoCliente.PF)));

        assertEquals("CPF informado e invalido", exception.getMessage());
    }

    @Test
    void deveFalharAoCadastrarClienteSemDocumentoQuandoTipoForInformado() {
        TestClienteRepository repository = new TestClienteRepository();
        CadastrarClienteService service = new CadastrarClienteService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarCliente(new CadastrarClienteCommand("Maria", null, TipoCliente.PJ)));

        assertEquals("Documento do cliente e obrigatorio quando o tipo for informado", exception.getMessage());
    }

    @Test
    void deveFalharAoCadastrarClienteSemTipoQuandoDocumentoForInformado() {
        TestClienteRepository repository = new TestClienteRepository();
        CadastrarClienteService service = new CadastrarClienteService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarCliente(new CadastrarClienteCommand("Maria", "12345678909", null)));

        assertEquals("Tipo do cliente e obrigatorio quando o documento for informado", exception.getMessage());
    }

    @Test
    void deveFalharAoCadastrarClienteSemNome() {
        TestClienteRepository repository = new TestClienteRepository();
        CadastrarClienteService service = new CadastrarClienteService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarCliente(new CadastrarClienteCommand(" ", "12345678909", TipoCliente.PF)));

        assertEquals("Nome do cliente e obrigatorio", exception.getMessage());
    }

    @Test
    void deveFalharAoCadastrarClienteComMesmoDocumentoMesmoComNomeDiferente() {
        TestClienteRepository repository = new TestClienteRepository();
        repository.salvar(Cliente.reconstituir(
                java.util.UUID.randomUUID(), "Maria Silva", "12345678909", TipoCliente.PF));
        CadastrarClienteService service = new CadastrarClienteService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarCliente(new CadastrarClienteCommand("Ana Souza", "123.456.789-09", TipoCliente.PF)));

        assertEquals("Ja existe cliente cadastrado com o mesmo CPF ou CNPJ.", exception.getMessage());
    }
}
