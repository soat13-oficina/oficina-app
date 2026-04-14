package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;

@Repository
@Transactional(readOnly = true)
public class JpaFuncionarioRepository implements FuncionarioRepository {
    private final SpringDataFuncionarioRepository repository;

    public JpaFuncionarioRepository(SpringDataFuncionarioRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Funcionario salvar(Funcionario funcionario) {
        return repository.save(funcionario);
    }

    @Override
    public Optional<Funcionario> buscarPorId(UUID id) {
        return repository.findById(id);
    }
}
