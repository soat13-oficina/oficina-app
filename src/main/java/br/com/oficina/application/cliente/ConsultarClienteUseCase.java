package br.com.oficina.application.cliente;

import java.util.Optional;

import br.com.oficina.domain.model.cliente.Cliente;

public interface ConsultarClienteUseCase {
    Optional<Cliente> consultarCliente(ConsultarClienteRequest request);

    record ConsultarClienteRequest(String clienteId) {
    }
}
