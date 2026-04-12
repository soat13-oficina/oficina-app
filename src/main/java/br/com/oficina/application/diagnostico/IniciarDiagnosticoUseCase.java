package br.com.oficina.application.diagnostico;

public interface IniciarDiagnosticoUseCase {
    void iniciarDiagnostico(IniciarDiagnosticoRequest request);

    record IniciarDiagnosticoRequest(String ordemDeServicoId) {
    }
}
