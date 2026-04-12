package br.com.oficina.cliente.application.usecase;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;

public interface CadastrarClienteUseCase {
    void cadastrarCliente(CadastrarClienteCommand command);
}
