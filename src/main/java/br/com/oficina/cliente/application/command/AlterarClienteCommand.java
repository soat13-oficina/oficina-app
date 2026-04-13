package br.com.oficina.cliente.application.command;

import br.com.oficina.cliente.domain.model.TipoCliente;

public record AlterarClienteCommand(String clienteId, String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
}
