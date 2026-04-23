package br.com.oficina.ordemservico.application.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.query.ConsultarFuncionarioQuery;
import br.com.oficina.ordemservico.application.usecase.ConsultarFuncionarioUseCase;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;

@Service
public class ConsultarFuncionarioService implements ConsultarFuncionarioUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConsultarFuncionarioService.class);

    private final FuncionarioRepository funcionarioRepository;

    public ConsultarFuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @Override
    public Optional<Funcionario> consultarFuncionario(ConsultarFuncionarioQuery query) {
        log.info("Consultando funcionario por identificador. funcionarioId={}", query.funcionarioId());
        Optional<Funcionario> funcionario = funcionarioRepository.buscarPorId(query.funcionarioId());
        log.info("Consulta de funcionario finalizada. funcionarioId={}, encontrado={}", query.funcionarioId(), funcionario.isPresent());
        return funcionario;
    }
}
