package br.com.oficina.ordemservico.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EnviarDiagnosticoParaOrcamentoUseCase {
    void enviarDiagnosticoParaOrcamento(EnviarDiagnosticoParaOrcamentoRequest request);

    // Apenas dados financeiros do orcamento; cliente, veiculo, funcionario, descricao do servico e
    // pecas sao derivados da OS/diagnostico (spec 014).
    record EnviarDiagnosticoParaOrcamentoRequest(
            String numeroOrdemServico,
            BigDecimal valorMaoDeObra,
            BigDecimal desconto,
            LocalDateTime validade,
            String observacoes) {
    }
}
