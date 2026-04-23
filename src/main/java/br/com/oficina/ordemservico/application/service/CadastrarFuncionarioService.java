package br.com.oficina.ordemservico.application.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.CadastrarFuncionarioCommand;
import br.com.oficina.ordemservico.application.usecase.CadastrarFuncionarioUseCase;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;

@Service
public class CadastrarFuncionarioService implements CadastrarFuncionarioUseCase {
    private static final Logger log = LoggerFactory.getLogger(CadastrarFuncionarioService.class);

    private final FuncionarioRepository funcionarioRepository;

    public CadastrarFuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    public UUID cadastrarFuncionario(CadastrarFuncionarioCommand command) {
        log.info("Iniciando cadastro de funcionario. cpfInformado={}", command.cpf() != null && !command.cpf().isBlank());
        validarDuplicidadeCpf(command.cpf(), null);
        Funcionario funcionario = funcionarioRepository.salvar(new Funcionario(command.nome(), command.cpf()));
        log.info("Funcionario cadastrado com sucesso. funcionarioId={}", funcionario.getId());
        return funcionario.getId();
    }

    private void validarDuplicidadeCpf(String cpf, UUID funcionarioIdAtual) {
        if (cpf == null || cpf.isBlank()) {
            return;
        }
        funcionarioRepository.buscarPorCpf(cpf)
                .filter(existente -> funcionarioIdAtual == null || !existente.getId().equals(funcionarioIdAtual))
                .ifPresent(existente -> {
                    throw new RegraDeNegocioException("Ja existe funcionario cadastrado com o mesmo CPF.");
                });
    }
}
