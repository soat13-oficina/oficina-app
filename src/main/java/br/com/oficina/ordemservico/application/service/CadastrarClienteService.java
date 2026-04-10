package br.com.oficina.ordemservico.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.usecase.CadastrarClienteUseCase;
import br.com.oficina.ordemservico.domain.model.Cliente;
import br.com.oficina.ordemservico.domain.repository.ClienteRepository;

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
