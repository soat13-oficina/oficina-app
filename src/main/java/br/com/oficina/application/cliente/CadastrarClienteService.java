package br.com.oficina.application.cliente;

import org.springframework.stereotype.Service;

import br.com.oficina.application.cliente.CadastrarClienteUseCase;
import br.com.oficina.domain.model.cliente.Cliente;
import br.com.oficina.domain.repository.ClienteRepository;

@Service
public class CadastrarClienteService implements CadastrarClienteUseCase {
    private final ClienteRepository clienteRepository;

    public CadastrarClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void cadastrarCliente(CadastrarClienteRequest request) {
        clienteRepository.salvar(new Cliente(request.id(), request.nome()));
    }
}
