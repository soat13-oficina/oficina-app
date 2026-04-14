package br.com.oficina.ordemservico.infrastructure.web.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnviarDiagnosticoParaOrcamentoRequest", description = "Dados do diagnóstico utilizados para gerar um orçamento")
public record EnviarDiagnosticoParaOrcamentoRequest(
        String descricaoDiagnostico,
        List<String> servicosPropostos,
        List<String> pecasPrevistas,
        BigDecimal valorMaoDeObra,
        BigDecimal valorPecas,
        LocalDateTime validade,
        String observacoes) {
    @Override
    @Schema(description = "Resumo textual do diagnóstico técnico", example = "Necessária troca de óleo e filtros")
    public String descricaoDiagnostico() {
        return descricaoDiagnostico;
    }

    @Override
    @Schema(description = "Lista de serviços propostos para execução")
    public List<String> servicosPropostos() {
        return servicosPropostos;
    }

    @Override
    @Schema(description = "Lista de peças previstas para o orçamento")
    public List<String> pecasPrevistas() {
        return pecasPrevistas;
    }

    @Override
    @Schema(description = "Valor estimado da mão de obra", example = "200.00")
    public BigDecimal valorMaoDeObra() {
        return valorMaoDeObra;
    }

    @Override
    @Schema(description = "Valor estimado das peças", example = "150.00")
    public BigDecimal valorPecas() {
        return valorPecas;
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
