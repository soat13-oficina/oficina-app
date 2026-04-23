package br.com.oficina.ordemservico.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.oficina.ordemservico.domain.model.Funcionario;

public interface FuncionarioRepository {
    Funcionario salvar(Funcionario funcionario);

    void atualizar(Funcionario funcionario);

    void excluirPorId(UUID id);

    Optional<Funcionario> buscarPorId(UUID id);

    List<Funcionario> buscarTodos();

    Optional<Funcionario> buscarPorCpf(String cpf);
}
