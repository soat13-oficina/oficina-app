package br.com.oficina.cliente.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.query.ConsultarClienteQuery;
import br.com.oficina.cliente.application.usecase.ConsultarClienteUseCase;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

@Service
public class ConsultarClienteService implements ConsultarClienteUseCase {
    private final ClienteRepository clienteRepository;

    public ConsultarClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Optional<Cliente> consultarCliente(ConsultarClienteQuery query) {
        return clienteRepository.buscarPorId(query.clienteId());
    }
}
