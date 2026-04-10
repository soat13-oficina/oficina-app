package br.com.oficina.ordemservico.application.usecase;

import java.util.Optional;

import br.com.oficina.ordemservico.domain.model.Cliente;

public interface ConsultarClienteUseCase {
    Optional<Cliente> consultarCliente(ConsultarClienteRequest request);

    record ConsultarClienteRequest(String clienteId) {
    }
}
