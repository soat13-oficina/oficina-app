package br.com.oficina.ordemservico.application.usecase;

public interface EnviarDiagnosticoParaOrcamentoUseCase {
    void enviarDiagnosticoParaOrcamento(EnviarDiagnosticoParaOrcamentoRequest request);

    record EnviarDiagnosticoParaOrcamentoRequest(String ordemDeServicoId) {
    }
}
