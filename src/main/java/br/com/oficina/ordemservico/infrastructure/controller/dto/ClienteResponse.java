package br.com.oficina.ordemservico.infrastructure.controller.dto;

import br.com.oficina.ordemservico.domain.model.Cliente;

public record ClienteResponse(String id, String nome) {
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome());
    }
}
