package br.com.oficina.veiculo.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Repository
@Transactional(readOnly = true)
public class JpaVeiculoRepository implements VeiculoRepository {
    private final SpringDataVeiculoRepository repository;

    public JpaVeiculoRepository(SpringDataVeiculoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvar(Veiculo veiculo) {
        repository.save(VeiculoJpaEntity.fromDomain(veiculo));
    }

    @Override
    public Optional<Veiculo> buscarPorPlaca(String placa) {
        return repository.findById(placa).map(VeiculoJpaEntity::toDomain);
    }

    @Override
    public List<Veiculo> buscarTodos() {
        return repository.findAll().stream().map(VeiculoJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void excluirPorPlaca(String placa) {
        repository.deleteById(placa);
    }
}
