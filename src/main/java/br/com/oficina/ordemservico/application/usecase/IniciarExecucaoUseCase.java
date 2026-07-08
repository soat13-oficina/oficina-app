package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.IniciarExecucaoCommand;

public interface IniciarExecucaoUseCase {
    void iniciarExecucao(IniciarExecucaoCommand command);
}
