package br.com.oficina.cliente.application.usecase;

import java.util.Optional;

import br.com.oficina.cliente.application.query.ConsultarClienteQuery;
import br.com.oficina.cliente.domain.model.Cliente;

public interface ConsultarClienteUseCase {
    Optional<Cliente> consultarCliente(ConsultarClienteQuery query);
}
