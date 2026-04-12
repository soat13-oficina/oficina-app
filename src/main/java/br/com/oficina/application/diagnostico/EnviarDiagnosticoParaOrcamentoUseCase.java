package br.com.oficina.application.diagnostico;

public interface EnviarDiagnosticoParaOrcamentoUseCase {
    void enviarDiagnosticoParaOrcamento(EnviarDiagnosticoParaOrcamentoRequest request);

    record EnviarDiagnosticoParaOrcamentoRequest(String ordemDeServicoId) {
    }
}
