package br.com.oficina.orcamento.infrastructure.web.response;

import br.com.oficina.orcamento.application.result.DecisaoOrcamentoResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DecisaoOrcamentoResponse", description = "Resultado da decisão externa de orçamento")
public record DecisaoOrcamentoResponse(
        String numeroOrcamento,
        String numeroOrdemServico,
        String situacao) {
    public static DecisaoOrcamentoResponse from(DecisaoOrcamentoResult result) {
        return new DecisaoOrcamentoResponse(
                result.numeroOrcamento(),
                result.numeroOrdemServico(),
                result.situacao());
    }
}
