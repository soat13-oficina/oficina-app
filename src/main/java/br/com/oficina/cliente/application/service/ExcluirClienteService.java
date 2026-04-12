package br.com.oficina.cliente.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.command.ExcluirClienteCommand;
import br.com.oficina.cliente.application.usecase.ExcluirClienteUseCase;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

@Service
public class ExcluirClienteService implements ExcluirClienteUseCase {
    private final ClienteRepository clienteRepository;

    public ExcluirClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }


    @Override
    public void excluirCliente(ExcluirClienteCommand command) {
        clienteRepository.excluirPorId(command.clienteId());
    }
}
