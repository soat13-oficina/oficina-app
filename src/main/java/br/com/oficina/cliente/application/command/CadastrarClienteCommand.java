package br.com.oficina.cliente.application.command;

import br.com.oficina.cliente.domain.model.TipoCliente;

public record CadastrarClienteCommand(String nome, String cpfOuCnpj, TipoCliente tipoCliente, String email) {
    public CadastrarClienteCommand(String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        this(nome, cpfOuCnpj, tipoCliente, null);
    }
}
