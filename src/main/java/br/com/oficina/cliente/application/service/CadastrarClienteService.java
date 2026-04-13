package br.com.oficina.cliente.application.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;
import br.com.oficina.cliente.application.usecase.CadastrarClienteUseCase;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

@Service
public class CadastrarClienteService implements CadastrarClienteUseCase {
    private static final Logger log = LoggerFactory.getLogger(CadastrarClienteService.class);

    private final ClienteRepository clienteRepository;

    public CadastrarClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public UUID cadastrarCliente(CadastrarClienteCommand command) {
        log.info("Iniciando cadastro de cliente. tipoCliente={}, documentoInformado={}",
                command.tipoCliente(),
                command.cpfOuCnpj() != null && !command.cpfOuCnpj().isBlank());
        Cliente clienteSalvo = clienteRepository.salvar(new Cliente(
                command.nome(),
                command.cpfOuCnpj(),
                command.tipoCliente()));
        log.info("Cliente cadastrado com sucesso. clienteId={}", clienteSalvo.getId());
        return clienteSalvo.getId();
    }
}
