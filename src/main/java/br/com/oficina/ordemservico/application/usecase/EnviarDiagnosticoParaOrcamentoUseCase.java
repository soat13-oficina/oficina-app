package br.com.oficina.ordemservico.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface EnviarDiagnosticoParaOrcamentoUseCase {
    void enviarDiagnosticoParaOrcamento(EnviarDiagnosticoParaOrcamentoRequest request);

    record EnviarDiagnosticoParaOrcamentoRequest(
            String numeroOrdemServico,
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<String> pecasPrevistas,
            BigDecimal valorMaoDeObra,
            BigDecimal valorPecas,
            LocalDateTime validade,
            String observacoes) {
    }
}
