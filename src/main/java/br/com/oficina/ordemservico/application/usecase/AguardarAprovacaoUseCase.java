package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.AguardarAprovacaoCommand;

public interface AguardarAprovacaoUseCase {
    void aguardarAprovacao(AguardarAprovacaoCommand command);
}
