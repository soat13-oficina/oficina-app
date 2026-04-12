package br.com.oficina.application.cliente;

import br.com.oficina.application.cliente.ExcluirClienteUseCase;
import br.com.oficina.domain.repository.ClienteRepository;
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
