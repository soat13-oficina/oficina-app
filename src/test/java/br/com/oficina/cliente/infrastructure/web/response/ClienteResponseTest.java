package br.com.oficina.cliente.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;

class ClienteResponseTest {

    @Test
    void deveConverterClienteParaResponse() {
        UUID clienteId = UUID.fromString("13131313-1313-1313-1313-131313131313");
        Cliente cliente = Cliente.reconstituir(clienteId, "Maria", "12345678909", TipoCliente.PF);

        ClienteResponse response = ClienteResponse.from(cliente);

        assertEquals(clienteId, response.id());
        assertEquals("Maria", response.nome());
        assertEquals("12345678909", response.cpfOuCnpj());
        assertEquals(TipoCliente.PF, response.tipoCliente());
    }
}
