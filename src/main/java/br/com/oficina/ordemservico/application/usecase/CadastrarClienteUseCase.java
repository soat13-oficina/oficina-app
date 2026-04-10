package br.com.oficina.ordemservico.application.usecase;

public interface CadastrarClienteUseCase {
    void cadastrarCliente(CadastrarClienteRequest request);

    record CadastrarClienteRequest(String id, String nome) {
    }
}
