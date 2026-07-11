package br.com.oficina.ordemservico.infrastructure.web.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnviarDiagnosticoParaOrcamentoRequest",
        description = "Dados financeiros do orçamento. Cliente, veículo, funcionário, descrição do serviço e peças são derivados da OS/diagnóstico.")
public record EnviarDiagnosticoParaOrcamentoRequest(
        BigDecimal valorMaoDeObra,
        BigDecimal desconto,
        LocalDateTime validade,
        String observacoes) {

    @Override
    @Schema(description = "Valor estimado da mão de obra", example = "200.00")
    public BigDecimal valorMaoDeObra() {
        return valorMaoDeObra;
    }

    @Override
    @Schema(description = "Valor de desconto aplicado ao serviço", example = "25.00")
    public BigDecimal desconto() {
        return desconto;
    }

    @Override
    @Schema(description = "Data de validade do orçamento gerado", example = "2030-01-01T00:00:00")
    public LocalDateTime validade() {
        return validade;
    }

    @Override
    @Schema(description = "Observações adicionais do orçamento", example = "Sujeito à aprovação do cliente")
    public String observacoes() {
        return observacoes;
    }
}
