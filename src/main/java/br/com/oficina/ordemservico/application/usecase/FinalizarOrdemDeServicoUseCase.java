package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.FinalizarOrdemDeServicoCommand;

public interface FinalizarOrdemDeServicoUseCase {
    void finalizarOrdemDeServico(FinalizarOrdemDeServicoCommand command);
}
