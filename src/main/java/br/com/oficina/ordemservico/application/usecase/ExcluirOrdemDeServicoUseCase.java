package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.ExcluirOrdemDeServicoCommand;

public interface ExcluirOrdemDeServicoUseCase {
    void excluirOrdemDeServico(ExcluirOrdemDeServicoCommand command);
}
