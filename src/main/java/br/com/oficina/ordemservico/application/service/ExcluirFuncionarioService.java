package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ExcluirFuncionarioCommand;
import br.com.oficina.ordemservico.application.usecase.ExcluirFuncionarioUseCase;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;

@Service
public class ExcluirFuncionarioService implements ExcluirFuncionarioUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExcluirFuncionarioService.class);

    private final FuncionarioRepository funcionarioRepository;

    public ExcluirFuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    public void excluirFuncionario(ExcluirFuncionarioCommand command) {
        log.info("Iniciando exclusao de funcionario. funcionarioId={}", command.funcionarioId());
        funcionarioRepository.buscarPorId(command.funcionarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario nao encontrado para o identificador informado."));
        funcionarioRepository.excluirPorId(command.funcionarioId());
        log.info("Funcionario excluido com sucesso. funcionarioId={}", command.funcionarioId());
    }
}
