package br.com.oficina.cliente.infrastructure.web.request;

import java.util.UUID;

import br.com.oficina.cliente.application.command.AlterarClienteCommand;
import br.com.oficina.cliente.domain.model.TipoCliente;

public record AlterarClienteRequest(String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
    public AlterarClienteCommand toCommand(UUID clienteId) {
        return new AlterarClienteCommand(clienteId, nome, cpfOuCnpj, tipoCliente);
    }
}
