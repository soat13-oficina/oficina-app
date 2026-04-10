package br.com.oficina.ordemservico.application.service;

import br.com.oficina.ordemservico.application.usecase.ExcluirClienteUseCase;
import br.com.oficina.ordemservico.domain.repository.ClienteRepository;
import org.springframework.stereotype.Service;

@Service
public class ExcluirClienteService implements ExcluirClienteUseCase {
    private final ClienteRepository clienteRepository;

    public ExcluirClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }


    @Override
    public void excluirCliente(ExcluirClienteRequest request) {
        clienteRepository.excluirPorId(request.clienteId());
    }
}
