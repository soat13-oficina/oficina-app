package br.com.oficina.orcamento.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Repository
@Transactional(readOnly = true)
public class JpaOrcamentoRepository implements OrcamentoRepository {
    private final SpringDataOrcamentoRepository repository;

    public JpaOrcamentoRepository(SpringDataOrcamentoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvar(Orcamento orcamento) {
        repository.save(OrcamentoJpaEntity.fromDomain(orcamento));
    }

    @Override
    @Transactional
    public void atualizar(Orcamento orcamento) {
        repository.save(OrcamentoJpaEntity.fromDomain(orcamento));
    }

    @Override
    @Transactional
    public void excluirPorId(String orcamentoId) {
        repository.deleteById(orcamentoId);
    }

    @Override
    public Optional<Orcamento> buscarPorId(String orcamentoId) {
        return repository.findById(orcamentoId).map(OrcamentoJpaEntity::toDomain);
    }

    @Override
    public List<Orcamento> buscarTodos() {
        return repository.findAll().stream().map(OrcamentoJpaEntity::toDomain).toList();
    }
}
