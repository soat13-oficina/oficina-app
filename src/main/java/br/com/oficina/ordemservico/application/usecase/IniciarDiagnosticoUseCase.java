package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.IniciarDiagnosticoCommand;

public interface IniciarDiagnosticoUseCase {
    void iniciarDiagnostico(IniciarDiagnosticoCommand command);
}
