package br.com.oficina.ordemservico.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.oficina.orcamento.domain.model.PecaOrcamento;

public interface EnviarDiagnosticoParaOrcamentoUseCase {
    void enviarDiagnosticoParaOrcamento(EnviarDiagnosticoParaOrcamentoRequest request);

    record EnviarDiagnosticoParaOrcamentoRequest(
            String numeroOrdemServico,
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<PecaOrcamento> pecasPrevistas,
            BigDecimal valorMaoDeObra,
            BigDecimal desconto,
            LocalDateTime validade,
            String observacoes) {
    }
}
