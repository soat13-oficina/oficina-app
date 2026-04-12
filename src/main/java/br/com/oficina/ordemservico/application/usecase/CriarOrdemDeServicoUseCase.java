package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;

public interface CriarOrdemDeServicoUseCase {
    void criarOrdemDeServico(CriarOrdemDeServicoCommand command);
}
