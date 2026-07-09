package br.com.oficina.cliente.infrastructure.web.request;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;
import br.com.oficina.cliente.domain.model.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CadastrarClienteRequest", description = "Dados necessários para cadastrar um cliente")
public record CadastrarClienteRequest(String nome, String cpfOuCnpj, TipoCliente tipoCliente, String email) {
    @Override
    @Schema(description = "Nome do cliente", example = "Maria da Silva")
    public String nome() {
        return nome;
    }

    @Override
    @Schema(description = "CPF ou CNPJ do cliente", example = "12345678909")
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

    public CadastrarClienteCommand toCommand() {
        return new CadastrarClienteCommand(nome, cpfOuCnpj, tipoCliente, email);
    }
}
