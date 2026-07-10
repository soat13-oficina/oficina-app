package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.ConcluirServicoCommand;

public interface ConcluirServicoUseCase {
    void concluirServico(ConcluirServicoCommand command);
}
