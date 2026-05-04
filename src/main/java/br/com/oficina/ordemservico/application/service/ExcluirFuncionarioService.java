package br.com.oficina.ordemservico.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ExcluirFuncionarioCommand;
import br.com.oficina.ordemservico.application.usecase.ExcluirFuncionarioUseCase;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Service
public class ExcluirFuncionarioService implements ExcluirFuncionarioUseCase {
    private static final Logger log = LoggerFactory.getLogger(ExcluirFuncionarioService.class);

    private final FuncionarioRepository funcionarioRepository;
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public ExcluirFuncionarioService(FuncionarioRepository funcionarioRepository,
            OrdemDeServicoRepository ordemDeServicoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void excluirFuncionario(ExcluirFuncionarioCommand command) {
        log.info("Iniciando exclusao de funcionario. funcionarioId={}", command.funcionarioId());
        funcionarioRepository.buscarPorId(command.funcionarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario nao encontrado para o identificador informado."));
        if (ordemDeServicoRepository.existePorFuncionarioId(command.funcionarioId())) {
            throw new RegraDeNegocioException("Nao e possivel excluir funcionario vinculado a ordens de servico.");
        }
        funcionarioRepository.excluirPorId(command.funcionarioId());
        log.info("Funcionario excluido com sucesso. funcionarioId={}", command.funcionarioId());
    }
}
