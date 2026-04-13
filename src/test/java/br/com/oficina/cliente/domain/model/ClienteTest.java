package br.com.oficina.cliente.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;

class ClienteTest {

    @Test
    void deveExporDadosDoClienteComDocumentoETipo() {
        Cliente cliente = new Cliente("cliente-1", "Maria", "12345678901", TipoCliente.PF);

        assertEquals("cliente-1", cliente.getId());
        assertEquals("Maria", cliente.getNome());
        assertEquals("12345678901", cliente.getCpfOuCnpj());
        assertEquals(TipoCliente.PF, cliente.getTipoCliente());
    }

    @Test
    void devePermitirClienteSemDocumentoNoConstrutorSimplificado() {
        Cliente cliente = new Cliente("cliente-2", "Joao");

        assertEquals("cliente-2", cliente.getId());
        assertEquals("Joao", cliente.getNome());
        assertNull(cliente.getCpfOuCnpj());
        assertNull(cliente.getTipoCliente());
    }

    @Test
    void deveAceitarCpfFormatadoQuandoPossuirOnzeDigitos() {
        Cliente cliente = new Cliente("cliente-3", "Ana", "123.456.789-01", TipoCliente.PF);

        assertEquals("123.456.789-01", cliente.getCpfOuCnpj());
        assertEquals(TipoCliente.PF, cliente.getTipoCliente());
    }

    @Test
    void deveFalharQuandoCpfNaoPossuirOnzeDigitos() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new Cliente("cliente-4", "Carlos", "1234567890", TipoCliente.PF));

        assertEquals("CPF deve possuir 11 digitos", exception.getMessage());
    }

    @Test
    void deveFalharQuandoCnpjNaoPossuirQuatorzeDigitos() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new Cliente("cliente-5", "Oficina", "1234567800019", TipoCliente.PJ));

        assertEquals("CNPJ deve possuir 14 digitos", exception.getMessage());
    }
}
