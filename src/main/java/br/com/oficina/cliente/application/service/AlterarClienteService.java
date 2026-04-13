package br.com.oficina.cliente.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.command.AlterarClienteCommand;
import br.com.oficina.cliente.application.usecase.AlterarClienteUseCase;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;

@Service
public class AlterarClienteService implements AlterarClienteUseCase {
    private final ClienteRepository clienteRepository;

    public AlterarClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void alterarCliente(AlterarClienteCommand command) {
        Cliente cliente = clienteRepository.buscarPorId(command.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));

        cliente.alterar(command.nome(), command.cpfOuCnpj(), command.tipoCliente());
        clienteRepository.atualizar(cliente);
    }
}
