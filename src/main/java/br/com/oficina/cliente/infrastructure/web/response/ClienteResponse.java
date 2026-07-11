package br.com.oficina.cliente.infrastructure.web.response;

import java.util.UUID;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ClienteResponse", description = "Representação de um cliente")
public record ClienteResponse(UUID id, String nome, String cpfOuCnpj, TipoCliente tipoCliente, String email) {
    @Override
    @Schema(description = "Identificador do cliente", example = "8e221ff7-71b9-4c22-8a8d-f94b6fd897cd")
    public UUID id() {
        return id;
    }

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

    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getCpfOuCnpj(),
                cliente.getTipoCliente(),
                cliente.getEmail());
    }
}
