package br.com.oficina.cliente.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.command.ExcluirClienteCommand;
import br.com.oficina.cliente.application.usecase.ExcluirClienteUseCase;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

@Service
public class ExcluirClienteService implements ExcluirClienteUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExcluirClienteService.class);

    private final ClienteRepository clienteRepository;

    public ExcluirClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }
    @Override
    public void excluirCliente(ExcluirClienteCommand command) {
        log.info("Iniciando exclusao de cliente. clienteId={}", command.clienteId());
        clienteRepository.buscarPorId(command.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));
        clienteRepository.excluirPorId(command.clienteId());
        log.info("Cliente excluido com sucesso. clienteId={}", command.clienteId());
    }
}
