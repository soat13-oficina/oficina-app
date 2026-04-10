package br.com.oficina.ordemservico.application.usecase;

public interface AlterarClienteUseCase {
    void alterarCliente(AlterarClienteRequest request);

    record AlterarClienteRequest(String clienteId, String nome) {
    }
}
