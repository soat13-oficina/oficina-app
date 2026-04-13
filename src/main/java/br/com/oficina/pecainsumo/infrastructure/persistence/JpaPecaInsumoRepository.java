package br.com.oficina.pecainsumo.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

@Repository
@Transactional(readOnly = true)
public class JpaPecaInsumoRepository implements PecaInsumoRepository {
    private final SpringDataPecaInsumoRepository repository;

    public JpaPecaInsumoRepository(SpringDataPecaInsumoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvar(PecaInsumo pecaInsumo) {
        repository.save(PecaInsumoJpaEntity.fromDomain(pecaInsumo));
    }

    @Override
    public Optional<PecaInsumo> buscarPorId(String id) {
        return repository.findById(id).map(PecaInsumoJpaEntity::toDomain);
    }

    @Override
    public List<PecaInsumo> buscarTodos() {
        return repository.findAll().stream().map(PecaInsumoJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void excluirPorId(String id) {
        repository.deleteById(id);
    }
}
