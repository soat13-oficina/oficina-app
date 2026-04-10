package br.com.oficina.ordemservico.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.usecase.AlterarClienteUseCase;
import br.com.oficina.ordemservico.domain.model.Cliente;
import br.com.oficina.ordemservico.domain.repository.ClienteRepository;

@Service
public class AlterarClienteService implements AlterarClienteUseCase {
    private final ClienteRepository clienteRepository;

    public AlterarClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void alterarCliente(AlterarClienteRequest request) {
        clienteRepository.buscarPorId(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado"));

        Cliente clienteAtualizado = new Cliente(request.clienteId(), request.nome());
        clienteRepository.atualizar(clienteAtualizado);
    }
}
