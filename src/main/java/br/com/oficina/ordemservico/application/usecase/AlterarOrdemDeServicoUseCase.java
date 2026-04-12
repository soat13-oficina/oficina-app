package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.AlterarOrdemDeServicoCommand;

public interface AlterarOrdemDeServicoUseCase {
    void alterarOrdemDeServico(AlterarOrdemDeServicoCommand command);
}
