package br.com.oficina.cliente.application.usecase;

import java.util.UUID;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;

public interface CadastrarClienteUseCase {
    UUID cadastrarCliente(CadastrarClienteCommand command);
}
