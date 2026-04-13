package br.com.oficina.cliente.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;

class ClienteResponseTest {

    @Test
    void deveConverterClienteParaResponse() {
        Cliente cliente = new Cliente("cliente-1", "Maria", "12345678901", TipoCliente.PF);

        ClienteResponse response = ClienteResponse.from(cliente);

        assertEquals("cliente-1", response.id());
        assertEquals("Maria", response.nome());
        assertEquals("12345678901", response.cpfOuCnpj());
        assertEquals(TipoCliente.PF, response.tipoCliente());
    }
}
