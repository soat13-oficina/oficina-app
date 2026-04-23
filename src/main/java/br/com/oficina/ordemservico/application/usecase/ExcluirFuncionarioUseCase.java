package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.ExcluirFuncionarioCommand;

public interface ExcluirFuncionarioUseCase {
    void excluirFuncionario(ExcluirFuncionarioCommand command);
}
