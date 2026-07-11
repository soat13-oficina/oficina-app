package br.com.oficina.orcamento.application.usecase;

import br.com.oficina.orcamento.application.command.DecidirOrcamentoExternamenteCommand;
import br.com.oficina.orcamento.application.result.DecisaoOrcamentoResult;

public interface DecidirOrcamentoExternamenteUseCase {
    DecisaoOrcamentoResult decidir(DecidirOrcamentoExternamenteCommand command);
}
