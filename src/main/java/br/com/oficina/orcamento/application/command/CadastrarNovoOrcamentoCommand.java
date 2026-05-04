package br.com.oficina.orcamento.application.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CadastrarNovoOrcamentoCommand(
        String numeroOrcamento,
        String clienteId,
        String ordemDeServicoId,
        String funcionarioId,
        String placaVeiculo,
        String marcaVeiculo,
        String modeloVeiculo,
        String descricaoDiagnostico,
        List<String> servicosPropostos,
        List<PecaOrcamentoInput> pecasPrevistas,
        BigDecimal valorMaoDeObra,
        BigDecimal desconto,
        LocalDateTime validade,
        String observacoes) {
}

