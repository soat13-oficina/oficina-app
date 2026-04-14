package br.com.oficina.ordemservico.domain.repository;

import java.util.Optional;
import java.util.UUID;

import br.com.oficina.ordemservico.domain.model.Funcionario;

public interface FuncionarioRepository {
    Funcionario salvar(Funcionario funcionario);

    Optional<Funcionario> buscarPorId(UUID id);
}
