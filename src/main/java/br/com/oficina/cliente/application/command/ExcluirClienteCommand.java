package br.com.oficina.cliente.application.command;

import java.util.UUID;

public record ExcluirClienteCommand(UUID clienteId) {
}
