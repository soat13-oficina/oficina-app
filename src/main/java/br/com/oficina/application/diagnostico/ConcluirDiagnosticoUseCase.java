package br.com.oficina.application.diagnostico;

public interface ConcluirDiagnosticoUseCase {
    void concluirDiagnostico(ConcluirDiagnosticoRequest request);

    record ConcluirDiagnosticoRequest(String ordemDeServicoId) {
    }
}
