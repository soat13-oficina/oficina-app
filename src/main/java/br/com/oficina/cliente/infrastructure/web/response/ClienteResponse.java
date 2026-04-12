package br.com.oficina.cliente.infrastructure.web.response;

import br.com.oficina.cliente.domain.model.Cliente;

public record ClienteResponse(String id, String nome, String cpf) {
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getCpf());
    }
}
