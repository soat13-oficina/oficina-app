package br.com.oficina.ordemservico.application.usecase;

import java.util.Optional;

import br.com.oficina.ordemservico.application.query.ConsultarFuncionarioQuery;
import br.com.oficina.ordemservico.domain.model.Funcionario;

public interface ConsultarFuncionarioUseCase {
    Optional<Funcionario> consultarFuncionario(ConsultarFuncionarioQuery query);
}
