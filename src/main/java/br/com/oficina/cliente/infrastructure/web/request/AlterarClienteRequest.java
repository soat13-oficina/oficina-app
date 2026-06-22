package br.com.oficina.cliente.infrastructure.web.request;

import java.util.UUID;

import br.com.oficina.cliente.application.command.AlterarClienteCommand;
import br.com.oficina.cliente.domain.model.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AlterarClienteRequest", description = "Dados necessários para alterar um cliente")
public record AlterarClienteRequest(String nome, String cpfOuCnpj, TipoCliente tipoCliente, String email) {
    @Override
    @Schema(description = "Nome do cliente", example = "Maria de Souza")
    public String nome() {
        return nome;
    }

    @Override
    @Schema(description = "CPF ou CNPJ do cliente", example = "12345678901")
    public String cpfOuCnpj() {
        return cpfOuCnpj;
    }

    @Override
    @Schema(description = "Tipo do cliente", example = "PF")
    public TipoCliente tipoCliente() {
        return tipoCliente;
    }

    @Override
    @Schema(description = "E-mail do cliente para notificações", example = "maria@email.com")
    public String email() {
        return email;
    }

    public AlterarClienteCommand toCommand(UUID clienteId) {
        return new AlterarClienteCommand(clienteId, nome, cpfOuCnpj, tipoCliente, email);
    }
}
