package br.com.oficina.orcamento.application.usecase;

import br.com.oficina.orcamento.application.command.RejeitarOrcamentoCommand;

public interface RejeitarOrcamentoUseCase {
    void rejeitarOrcamento(RejeitarOrcamentoCommand command);
}
