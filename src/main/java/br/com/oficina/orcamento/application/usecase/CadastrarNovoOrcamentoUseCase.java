package br.com.oficina.orcamento.application.usecase;

import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;

public interface CadastrarNovoOrcamentoUseCase {
    void cadastrarNovoOrcamento(CadastrarNovoOrcamentoCommand command);
}
