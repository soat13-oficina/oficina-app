package br.com.oficina.support.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;

public class TestFuncionarioRepository implements FuncionarioRepository {
    private final Map<UUID, Funcionario> funcionarios = new ConcurrentHashMap<>();

    @Override
    public Funcionario salvar(Funcionario funcionario) {
        Funcionario funcionarioPersistido = funcionario.getId() == null
                ? Funcionario.reconstituir(UUID.randomUUID(), funcionario.getNome(), funcionario.getCpf())
                : funcionario;
        funcionarios.put(funcionarioPersistido.getId(), funcionarioPersistido);
        return funcionarioPersistido;
    }

    @Override
    public void atualizar(Funcionario funcionario) {
        funcionarios.put(funcionario.getId(), funcionario);
    }

    @Override
    public void excluirPorId(UUID id) {
        funcionarios.remove(id);
    }

    @Override
    public Optional<Funcionario> buscarPorId(UUID id) {
        return Optional.ofNullable(funcionarios.get(id));
    }

    @Override
    public List<Funcionario> buscarTodos() {
        return List.copyOf(funcionarios.values());
    }

    @Override
    public Optional<Funcionario> buscarPorCpf(String cpf) {
        String cpfNormalizado = cpf.replaceAll("\\D", "");
        return funcionarios.values().stream()
                .filter(funcionario -> funcionario.getCpf() != null
                        && funcionario.getCpf().replaceAll("\\D", "").equals(cpfNormalizado))
                .findFirst();
    }
}
