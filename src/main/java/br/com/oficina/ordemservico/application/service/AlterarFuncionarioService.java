package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.AlterarFuncionarioCommand;
import br.com.oficina.ordemservico.application.usecase.AlterarFuncionarioUseCase;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;

@Service
public class AlterarFuncionarioService implements AlterarFuncionarioUseCase {
    private static final Logger log = LoggerFactory.getLogger(AlterarFuncionarioService.class);

    private final FuncionarioRepository funcionarioRepository;

    public AlterarFuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    public void alterarFuncionario(AlterarFuncionarioCommand command) {
        log.info("Iniciando alteracao de funcionario. funcionarioId={}, cpfInformado={}",
                command.funcionarioId(),
                command.cpf() != null && !command.cpf().isBlank());

        Funcionario funcionario = funcionarioRepository.buscarPorId(command.funcionarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario nao encontrado para o identificador informado."));

        validarDuplicidadeCpf(command.cpf(), command.funcionarioId().toString());
        funcionario.alterar(command.nome(), command.cpf());
        funcionarioRepository.atualizar(funcionario);

        log.info("Funcionario alterado com sucesso. funcionarioId={}", command.funcionarioId());
    }

    private void validarDuplicidadeCpf(String cpf, String funcionarioIdAtual) {
        if (cpf == null || cpf.isBlank()) {
            return;
        }
        funcionarioRepository.buscarPorCpf(cpf)
                .filter(existente -> !existente.getId().toString().equals(funcionarioIdAtual))
                .ifPresent(existente -> {
                    throw new RegraDeNegocioException("Ja existe funcionario cadastrado com o mesmo CPF.");
                });
    }
}
