package br.com.oficina.ordemservico.application.usecase;

public interface ExcluirClienteUseCase {
    void excluirCliente(ExcluirClienteRequest request);

    record ExcluirClienteRequest(String clienteId) {
    }
}
