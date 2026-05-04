package br.com.oficina.cliente.application.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.query.ConsultarClienteQuery;
import br.com.oficina.cliente.application.usecase.ConsultarClienteUseCase;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

@Service
public class ConsultarClienteService implements ConsultarClienteUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConsultarClienteService.class);

    private final ClienteRepository clienteRepository;

    public ConsultarClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Optional<Cliente> consultarCliente(ConsultarClienteQuery query) {
        log.info("Consultando cliente por identificador. clienteId={}", query.clienteId());
        Optional<Cliente> cliente = clienteRepository.buscarPorId(query.clienteId());
        log.info("Consulta de cliente finalizada. clienteId={}, encontrado={}", query.clienteId(), cliente.isPresent());
        return cliente;
    }
}
