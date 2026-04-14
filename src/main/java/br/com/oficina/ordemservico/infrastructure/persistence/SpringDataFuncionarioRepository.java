package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.oficina.ordemservico.domain.model.Funcionario;

public interface SpringDataFuncionarioRepository extends JpaRepository<Funcionario, UUID> {
}
