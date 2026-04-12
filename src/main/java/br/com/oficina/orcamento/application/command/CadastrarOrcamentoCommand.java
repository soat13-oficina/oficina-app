package br.com.oficina.orcamento.application.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CadastrarOrcamentoCommand(
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
        LocalDateTime validade,
        String observacoes) {
}
