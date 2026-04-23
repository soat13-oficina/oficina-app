package br.com.oficina.ordemservico.application.usecase;

import java.util.UUID;

import br.com.oficina.ordemservico.application.command.CadastrarFuncionarioCommand;

public interface CadastrarFuncionarioUseCase {
    UUID cadastrarFuncionario(CadastrarFuncionarioCommand command);
}
