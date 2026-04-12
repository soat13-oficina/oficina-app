package br.com.oficina.cliente.application.usecase;

import br.com.oficina.cliente.application.command.ExcluirClienteCommand;

public interface ExcluirClienteUseCase {
    void excluirCliente(ExcluirClienteCommand command);
}
