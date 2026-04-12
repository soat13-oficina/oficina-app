package br.com.oficina.orcamento.application.usecase;

import br.com.oficina.orcamento.application.command.ExcluirOrcamentoCommand;

public interface ExcluirOrcamentoUseCase {
    void excluirOrcamento(ExcluirOrcamentoCommand command);
}
