package br.com.oficina.orcamento.infrastructure.web.request;

import br.com.oficina.orcamento.application.command.DecidirOrcamentoExternamenteCommand;
import br.com.oficina.orcamento.domain.model.DecisaoOrcamento;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DecisaoOrcamentoRequest", description = "Decisão externa de aprovação ou recusa do orçamento")
public record DecisaoOrcamentoRequest(DecisaoOrcamento decisao) {
    public DecidirOrcamentoExternamenteCommand toCommand(String numeroOrcamento) {
        return new DecidirOrcamentoExternamenteCommand(numeroOrcamento, decisao);
    }
}
