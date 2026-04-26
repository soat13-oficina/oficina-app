package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.EntregarAoClienteCommand;

public interface EntregarAoClienteUseCase {
    void entregarAoCliente(EntregarAoClienteCommand command);
}
