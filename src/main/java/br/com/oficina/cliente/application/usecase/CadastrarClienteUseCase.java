package br.com.oficina.cliente.application.usecase;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;

public interface CadastrarClienteUseCase {
    String cadastrarCliente(CadastrarClienteCommand command);
}
