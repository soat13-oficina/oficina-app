package br.com.oficina.ordemservico.application.usecase;

public interface ConcluirDiagnosticoUseCase {
    void concluirDiagnostico(ConcluirDiagnosticoRequest request);

    record ConcluirDiagnosticoRequest(String ordemDeServicoId) {
    }
}
