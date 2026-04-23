package br.com.oficina.ordemservico.application.usecase;

import java.util.List;

import br.com.oficina.ordemservico.application.query.ListarFuncionariosQuery;
import br.com.oficina.ordemservico.domain.model.Funcionario;

public interface ListarFuncionariosUseCase {
    List<Funcionario> listarFuncionarios(ListarFuncionariosQuery query);
}
