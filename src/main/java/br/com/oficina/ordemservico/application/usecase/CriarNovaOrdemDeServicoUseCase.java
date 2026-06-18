package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;

public interface CriarNovaOrdemDeServicoUseCase {
    String criarNovaOrdemDeServico(CriarOrdemDeServicoCommand command);
}
