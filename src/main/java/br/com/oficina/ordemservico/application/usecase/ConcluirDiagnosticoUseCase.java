package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.ConcluirDiagnosticoCommand;

public interface ConcluirDiagnosticoUseCase {
    void concluirDiagnostico(ConcluirDiagnosticoCommand command);
}
