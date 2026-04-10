package br.com.oficina.ordemservico.application.usecase;

public interface IniciarDiagnosticoUseCase {
    void iniciarDiagnostico(IniciarDiagnosticoRequest request);

    record IniciarDiagnosticoRequest(String ordemDeServicoId) {
    }
}
