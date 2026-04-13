package br.com.oficina.cliente.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;
import br.com.oficina.cliente.application.usecase.CadastrarClienteUseCase;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

@Service
public class CadastrarClienteService implements CadastrarClienteUseCase {
    private final ClienteRepository clienteRepository;

    public CadastrarClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public UUID cadastrarCliente(CadastrarClienteCommand command) {
        Cliente clienteSalvo = clienteRepository.salvar(new Cliente(
                command.nome(),
                command.cpfOuCnpj(),
                command.tipoCliente()));
        return clienteSalvo.getId();
    }
}
