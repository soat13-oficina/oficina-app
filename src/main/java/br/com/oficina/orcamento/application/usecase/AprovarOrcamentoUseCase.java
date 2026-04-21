package br.com.oficina.orcamento.application.usecase;

import br.com.oficina.orcamento.application.command.AprovarOrcamentoCommand;
import br.com.oficina.orcamento.domain.model.Orcamento;

public interface AprovarOrcamentoUseCase {
    Orcamento aprovarOrcamento(AprovarOrcamentoCommand command);
}
