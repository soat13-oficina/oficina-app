package br.com.oficina.cliente.infrastructure.web.request;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;
import br.com.oficina.cliente.domain.model.TipoCliente;

public record CadastrarClienteRequest(String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
    public CadastrarClienteCommand toCommand() {
        return new CadastrarClienteCommand(nome, cpfOuCnpj, tipoCliente);
    }
}
