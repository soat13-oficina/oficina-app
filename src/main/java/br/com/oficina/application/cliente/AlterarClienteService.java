package br.com.oficina.application.cliente;

import org.springframework.stereotype.Service;

import br.com.oficina.application.cliente.AlterarClienteUseCase;
import br.com.oficina.domain.model.cliente.Cliente;
import br.com.oficina.domain.repository.ClienteRepository;

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
