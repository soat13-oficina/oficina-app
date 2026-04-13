package br.com.oficina.orcamento.application.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AlterarOrcamentoCommand(
        String numeroOrcamento,
        String clienteId,
        String ordemDeServicoId,
        String funcionarioId,
        String placaVeiculo,
        String marcaVeiculo,
        String modeloVeiculo,
        String descricaoDiagnostico,
        List<String> servicosPropostos,
        List<String> pecasPrevistas,
        BigDecimal valorMaoDeObra,
        BigDecimal valorPecas,
        LocalDateTime validade,
        String observacoes) {
}
