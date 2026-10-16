package br.com.oficina.orcamento.application.usecase;

import java.util.UUID;

import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;

public interface CadastrarNovoOrcamentoUseCase {

    /** @return id do orcamento criado */
    UUID cadastrarNovoOrcamento(CadastrarNovoOrcamentoCommand command);
}
