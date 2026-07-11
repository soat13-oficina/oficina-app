package br.com.oficina.orcamento.application.command;

import br.com.oficina.orcamento.domain.model.DecisaoOrcamento;

public record DecidirOrcamentoExternamenteCommand(String numeroOrcamento, DecisaoOrcamento decisao) {
}
