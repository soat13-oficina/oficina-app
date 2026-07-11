package br.com.oficina.cliente.application.command;

import java.util.UUID;

import br.com.oficina.cliente.domain.model.TipoCliente;

public record AlterarClienteCommand(UUID clienteId, String nome, String cpfOuCnpj, TipoCliente tipoCliente, String email) {
    public AlterarClienteCommand(UUID clienteId, String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        this(clienteId, nome, cpfOuCnpj, tipoCliente, null);
    }
}
