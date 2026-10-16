package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.AbortarExecucaoCommand;

public interface AbortarExecucaoUseCase {
    void abortarExecucao(AbortarExecucaoCommand command);
}
