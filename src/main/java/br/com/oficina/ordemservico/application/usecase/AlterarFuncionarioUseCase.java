package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.AlterarFuncionarioCommand;

public interface AlterarFuncionarioUseCase {
    void alterarFuncionario(AlterarFuncionarioCommand command);
}
