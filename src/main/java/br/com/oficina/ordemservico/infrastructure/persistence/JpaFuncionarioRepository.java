package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.List;
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
    @Transactional
    public void atualizar(Funcionario funcionario) {
        repository.save(funcionario);
    }

    @Override
    @Transactional
    public void excluirPorId(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Funcionario> buscarPorId(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<Funcionario> buscarTodos() {
        return repository.findAll();
    }

    @Override
    public Optional<Funcionario> buscarPorCpf(String cpf) {
        return repository.findByCpf(cpf.replaceAll("\\D", ""));
    }
}
