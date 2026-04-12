package br.com.oficina.application.cliente;

public interface ExcluirClienteUseCase {
    void excluirCliente(ExcluirClienteRequest request);

    record ExcluirClienteRequest(String clienteId) {
    }
}
