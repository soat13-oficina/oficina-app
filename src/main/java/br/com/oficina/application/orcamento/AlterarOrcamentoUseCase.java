package br.com.oficina.application.orcamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AlterarOrcamentoUseCase {
    void alterarOrcamento(AlterarOrcamentoRequest request);

    record AlterarOrcamentoRequest(
            String orcamentoId,
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
}
