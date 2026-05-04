package br.com.oficina.orcamento.application.usecase;

import br.com.oficina.orcamento.application.command.AlterarOrcamentoCommand;

public interface AlterarOrcamentoUseCase {
    void alterarOrcamento(AlterarOrcamentoCommand command);
}
