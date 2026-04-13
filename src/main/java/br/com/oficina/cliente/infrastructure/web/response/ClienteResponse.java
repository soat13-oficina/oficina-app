package br.com.oficina.cliente.infrastructure.web.response;

import java.util.UUID;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;

public record ClienteResponse(UUID id, String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCpfOuCnpj(),
                cliente.getTipoCliente());
    }
}
