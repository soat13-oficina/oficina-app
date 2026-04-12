package br.com.oficina.application.cliente;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.oficina.application.cliente.ConsultarClienteUseCase;
import br.com.oficina.domain.model.cliente.Cliente;
import br.com.oficina.domain.repository.ClienteRepository;

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
