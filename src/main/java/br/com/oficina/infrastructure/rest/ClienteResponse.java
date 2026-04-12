package br.com.oficina.infrastructure.rest;

import br.com.oficina.domain.model.cliente.Cliente;

public record ClienteResponse(String id, String nome) {
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome());
    }
}
