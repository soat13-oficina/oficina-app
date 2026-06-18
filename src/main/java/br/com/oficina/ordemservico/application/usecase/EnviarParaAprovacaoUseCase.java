package br.com.oficina.ordemservico.application.usecase;

import br.com.oficina.ordemservico.application.command.EnviarParaAprovacaoCommand;

public interface EnviarParaAprovacaoUseCase {
    void enviarParaAprovacao(EnviarParaAprovacaoCommand command);
}
