package br.com.oficina.application.cliente;

public interface AlterarClienteUseCase {
    void alterarCliente(AlterarClienteRequest request);

    record AlterarClienteRequest(String clienteId, String nome) {
    }
}
