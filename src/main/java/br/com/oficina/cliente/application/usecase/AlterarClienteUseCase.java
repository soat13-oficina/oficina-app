package br.com.oficina.cliente.application.usecase;

import br.com.oficina.cliente.application.command.AlterarClienteCommand;

public interface AlterarClienteUseCase {
    void alterarCliente(AlterarClienteCommand command);
}
