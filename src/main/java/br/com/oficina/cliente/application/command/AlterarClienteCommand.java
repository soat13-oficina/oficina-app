package br.com.oficina.cliente.application.command;

public record AlterarClienteCommand(String clienteId, String nome, String cpf) {
}
