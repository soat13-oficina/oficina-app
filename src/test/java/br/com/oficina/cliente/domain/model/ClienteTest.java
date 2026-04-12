package br.com.oficina.cliente.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ClienteTest {

    @Test
    void deveExporDadosDoClienteComCpf() {
        Cliente cliente = new Cliente("cliente-1", "Maria", "12345678901");

        assertEquals("cliente-1", cliente.getId());
        assertEquals("Maria", cliente.getNome());
        assertEquals("12345678901", cliente.getCpf());
    }

    @Test
    void devePermitirClienteSemCpfNoConstrutorSimplificado() {
        Cliente cliente = new Cliente("cliente-2", "Joao");

        assertEquals("cliente-2", cliente.getId());
        assertEquals("Joao", cliente.getNome());
        assertNull(cliente.getCpf());
    }
}
