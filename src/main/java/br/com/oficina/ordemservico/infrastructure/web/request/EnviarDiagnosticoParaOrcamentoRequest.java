package br.com.oficina.ordemservico.infrastructure.web.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.oficina.orcamento.application.command.PecaOrcamentoInput;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnviarDiagnosticoParaOrcamentoRequest", description = "Dados do diagnóstico utilizados para gerar um orçamento")
public record EnviarDiagnosticoParaOrcamentoRequest(
        String descricaoDiagnostico,
        List<String> servicosPropostos,
        List<PecaOrcamentoRequest> pecasPrevistas,
        BigDecimal valorMaoDeObra,
        BigDecimal desconto,
        LocalDateTime validade,
        String observacoes) {
    @Override
    @Schema(description = "Resumo textual do diagnóstico técnico", example = "Necessária troca de óleo e filtros")
    public String descricaoDiagnostico() {
        return descricaoDiagnostico;
    }

    @Override
    @Schema(description = "Lista de serviços propostos para execução", example = "[\"Troca de óleo\", \"Substituição do filtro de óleo\"]")
    public List<String> servicosPropostos() {
        return servicosPropostos;
    }

    @Override
    @Schema(description = "Lista de peças previstas referenciando o cadastro de peças/insumos")
    public List<PecaOrcamentoRequest> pecasPrevistas() {
        return pecasPrevistas;
    }

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

    public List<PecaOrcamentoInput> toPecasInput() {
        if (pecasPrevistas == null) {
            return List.of();
        }
        return pecasPrevistas.stream()
                .map(p -> new PecaOrcamentoInput(p.pecaInsumoId(), p.quantidade()))
                .toList();
    }

    @Schema(name = "PecaOrcamentoRequest", description = "Referência a uma peça/insumo cadastrada com quantidade desejada")
    public record PecaOrcamentoRequest(String pecaInsumoId, int quantidade) {
        @Override
        @Schema(description = "Identificador da peça/insumo no cadastro", example = "abc123")
        public String pecaInsumoId() {
            return pecaInsumoId;
        }

        @Override
        @Schema(description = "Quantidade necessária da peça", example = "2")
        public int quantidade() {
            return quantidade;
        }
    }
}
