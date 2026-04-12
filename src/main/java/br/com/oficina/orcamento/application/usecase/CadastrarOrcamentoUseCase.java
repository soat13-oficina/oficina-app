package br.com.oficina.orcamento.application.usecase;

import br.com.oficina.orcamento.application.command.CadastrarOrcamentoCommand;

public interface CadastrarOrcamentoUseCase {
    void cadastrarOrcamento(CadastrarOrcamentoCommand command);
}
