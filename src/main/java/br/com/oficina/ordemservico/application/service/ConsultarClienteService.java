package br.com.oficina.ordemservico.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.usecase.ConsultarClienteUseCase;
import br.com.oficina.ordemservico.domain.model.Cliente;
import br.com.oficina.ordemservico.domain.repository.ClienteRepository;

@Service
public class ConsultarClienteService implements ConsultarClienteUseCase {
    private final ClienteRepository clienteRepository;

    public ConsultarClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Optional<Cliente> consultarCliente(ConsultarClienteRequest request) {
        return clienteRepository.buscarPorId(request.clienteId());
    }
}
