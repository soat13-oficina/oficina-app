package br.com.oficina.infrastructure.rest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.oficina.domain.model.orcamento.Orcamento;

public record OrcamentoResponse(
        String id,
        String ordemDeServicoId,
        String funcionarioId,
        String clienteId,
        String placaVeiculo,
        String descricaoDiagnostico,
        List<String> servicosPropostos,
        List<String> pecasPrevistas,
        BigDecimal valorMaoDeObra,
        BigDecimal valorPecas,
        BigDecimal valorTotal,
        LocalDateTime criadoEm,
        LocalDateTime validade,
        String observacoes,
        LocalDateTime enviadoParaAprovacaoEm) {
    public static OrcamentoResponse from(Orcamento orcamento) {
        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getOrdemDeServicoId(),
                orcamento.getFuncionarioId(),
                orcamento.getClienteId(),
                orcamento.getPlacaVeiculo(),
                orcamento.getDescricaoDiagnostico(),
                orcamento.getServicosPropostos(),
                orcamento.getPecasPrevistas(),
                orcamento.getValorMaoDeObra(),
                orcamento.getValorPecas(),
                orcamento.getValorTotal(),
                orcamento.getCriadoEm(),
                orcamento.getValidade(),
                orcamento.getObservacoes(),
                orcamento.getEnviadoParaAprovacaoEm());
    }
}
