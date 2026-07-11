package br.com.oficina.cliente.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.application.command.ExcluirClienteCommand;
import br.com.oficina.cliente.application.usecase.ExcluirClienteUseCase;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.cliente.domain.repository.VerificadorVinculosCliente;
import br.com.oficina.common.domain.exception.ConflitoDeRecursoException;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;

@Service
public class ExcluirClienteService implements ExcluirClienteUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExcluirClienteService.class);

    private final ClienteRepository clienteRepository;
    private final VerificadorVinculosCliente verificadorVinculosCliente;

    public ExcluirClienteService(
            ClienteRepository clienteRepository,
            VerificadorVinculosCliente verificadorVinculosCliente) {
        this.clienteRepository = clienteRepository;
        this.verificadorVinculosCliente = verificadorVinculosCliente;
    }

    @Override
    public void excluirCliente(ExcluirClienteCommand command) {
        log.info("Iniciando exclusao de cliente. clienteId={}", command.clienteId());
        clienteRepository.buscarPorId(command.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));

        if (verificadorVinculosCliente.possuiVinculos(command.clienteId())) {
            log.info("Exclusao de cliente bloqueada por vinculos. clienteId={}", command.clienteId());
            throw new ConflitoDeRecursoException(
                    "Nao e possivel excluir o cliente: existem veiculos ou ordens de servico vinculados.");
        }

        clienteRepository.excluirPorId(command.clienteId());
        log.info("Cliente excluido com sucesso. clienteId={}", command.clienteId());
    }
}
