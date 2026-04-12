package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;

public interface CriarNovaOrdemDeServicoUseCase {
    void criarNovaOrdemDeServico(CriarOrdemDeServicoCommand command);
}
