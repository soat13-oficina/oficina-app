package br.com.oficina.cliente.application.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.command.CadastrarClienteCommand;
import br.com.oficina.cliente.application.usecase.CadastrarClienteUseCase;
import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;

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
        validarDuplicidade(command.cpfOuCnpj(), null);
        Cliente clienteSalvo = clienteRepository.salvar(new Cliente(
                command.nome(),
                command.cpfOuCnpj(),
                command.tipoCliente()));
        log.info("Cliente cadastrado com sucesso. clienteId={}", clienteSalvo.getId());
        return clienteSalvo.getId();
    }

    private void validarDuplicidade(String cpfOuCnpj, UUID clienteIdAtual) {
        if (cpfOuCnpj == null || cpfOuCnpj.isBlank()) {
            return;
        }

        clienteRepository.buscarPorDocumento(cpfOuCnpj)
                .filter(clienteExistente -> clienteIdAtual == null || !clienteExistente.getId().equals(clienteIdAtual))
                .ifPresent(clienteExistente -> {
                    throw new RegraDeNegocioException("Ja existe cliente cadastrado com o mesmo CPF ou CNPJ.");
                });
    }
}
