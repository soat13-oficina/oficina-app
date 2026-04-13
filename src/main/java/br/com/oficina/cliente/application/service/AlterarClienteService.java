package br.com.oficina.cliente.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.command.AlterarClienteCommand;
import br.com.oficina.cliente.application.usecase.AlterarClienteUseCase;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;

@Service
public class AlterarClienteService implements AlterarClienteUseCase {
    private static final Logger log = LoggerFactory.getLogger(AlterarClienteService.class);

    private final ClienteRepository clienteRepository;

    public AlterarClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void alterarCliente(AlterarClienteCommand command) {
        log.info("Iniciando alteracao de cliente. clienteId={}, tipoCliente={}, documentoInformado={}",
                command.clienteId(),
                command.tipoCliente(),
                command.cpfOuCnpj() != null && !command.cpfOuCnpj().isBlank());
        Cliente cliente = clienteRepository.buscarPorId(command.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));

        cliente.alterar(command.nome(), command.cpfOuCnpj(), command.tipoCliente());
        clienteRepository.atualizar(cliente);
        log.info("Cliente alterado com sucesso. clienteId={}", command.clienteId());
    }
}
