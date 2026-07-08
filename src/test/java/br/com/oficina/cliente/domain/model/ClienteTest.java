package br.com.oficina.cliente.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;

class ClienteTest {

    @Test
    void deveExporDadosDoClienteComDocumentoETipo() {
        UUID clienteId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Cliente cliente = Cliente.reconstituir(clienteId, "Maria", "12345678909", TipoCliente.PF);

        assertEquals(clienteId, cliente.getId());
        assertEquals("Maria", cliente.getNome());
        assertEquals("12345678909", cliente.getCpfOuCnpj());
        assertEquals(TipoCliente.PF, cliente.getTipoCliente());
    }

    @Test
    void devePermitirClienteSemDocumentoNoConstrutorSimplificado() {
        UUID clienteId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Cliente cliente = Cliente.reconstituir(clienteId, "Joao");

        assertEquals(clienteId, cliente.getId());
        assertEquals("Joao", cliente.getNome());
        assertNull(cliente.getCpfOuCnpj());
        assertNull(cliente.getTipoCliente());
    }

    @Test
    void deveAceitarCpfFormatadoQuandoPossuirOnzeDigitos() {
        Cliente cliente = Cliente.reconstituir(UUID.fromString("33333333-3333-3333-3333-333333333333"), "Ana", "123.456.789-09", TipoCliente.PF);

        assertEquals("123.456.789-09", cliente.getCpfOuCnpj());
        assertEquals(TipoCliente.PF, cliente.getTipoCliente());
    }

    @Test
    void deveFalharQuandoCpfNaoPossuirOnzeDigitos() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> Cliente.reconstituir(UUID.fromString("44444444-4444-4444-4444-444444444444"), "Carlos", "1234567890", TipoCliente.PF));

        assertEquals("CPF informado e invalido", exception.getMessage());
    }

    @Test
    void deveFalharQuandoCnpjNaoPossuirQuatorzeDigitos() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> Cliente.reconstituir(UUID.fromString("55555555-5555-5555-5555-555555555555"), "Oficina", "1234567800019", TipoCliente.PJ));

        assertEquals("CNPJ informado e invalido", exception.getMessage());
    }

    @Test
    void deveRejeitarCpfComOnzeDigitosMasDigitoVerificadorInvalido() {
        // "12345678901" tem 11 digitos, mas o digito verificador correto seria "...09".
        // Antes a validacao so contava digitos e aceitava este CPF; agora o DV e conferido.
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> Cliente.reconstituir(UUID.fromString("88888888-8888-8888-8888-888888888888"), "Carlos", "12345678901", TipoCliente.PF));

        assertEquals("CPF informado e invalido", exception.getMessage());
    }

    @Test
    void deveRejeitarCpfComDigitosRepetidos() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> Cliente.reconstituir(UUID.fromString("99999999-9999-9999-9999-999999999999"), "Carlos", "11111111111", TipoCliente.PF));

        assertEquals("CPF informado e invalido", exception.getMessage());
    }

    @Test
    void deveRejeitarCnpjComDigitoVerificadorInvalido() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> Cliente.reconstituir(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "Oficina", "11222333000199", TipoCliente.PJ));

        assertEquals("CNPJ informado e invalido", exception.getMessage());
    }

    @Test
    void deveAceitarCnpjValido() {
        Cliente cliente = Cliente.reconstituir(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Oficina", "11444777000161", TipoCliente.PJ);

        assertEquals("11444777000161", cliente.getCpfOuCnpj());
        assertEquals(TipoCliente.PJ, cliente.getTipoCliente());
    }

    @Test
    void deveFalharQuandoTipoForInformadoSemDocumento() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> Cliente.reconstituir(UUID.fromString("66666666-6666-6666-6666-666666666666"), "Maria", null, TipoCliente.PF));

        assertEquals("Documento do cliente e obrigatorio quando o tipo for informado", exception.getMessage());
    }

    @Test
    void deveFalharQuandoDocumentoForInformadoSemTipo() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> Cliente.reconstituir(UUID.fromString("77777777-7777-7777-7777-777777777777"), "Maria", "12345678909", null));

        assertEquals("Tipo do cliente e obrigatorio quando o documento for informado", exception.getMessage());
    }

    @Test
    void deveFalharQuandoNomeNaoForInformado() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new Cliente(" ", "12345678909", TipoCliente.PF));

        assertEquals("Nome do cliente e obrigatorio", exception.getMessage());
    }
}
