package br.com.oficina.application.cliente;

public interface CadastrarClienteUseCase {
    void cadastrarCliente(CadastrarClienteRequest request);

    record CadastrarClienteRequest(String id, String nome) {
    }
}
