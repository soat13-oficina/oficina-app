package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;

@Repository
@Transactional(readOnly = true)
public class JpaOrdemDeServicoRepository implements OrdemDeServicoRepository {
    private final SpringDataOrdemDeServicoRepository repository;

    public JpaOrdemDeServicoRepository(SpringDataOrdemDeServicoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvar(OrdemDeServico ordemDeServico) {
        repository.save(OrdemDeServicoJpaEntity.fromDomain(ordemDeServico));
    }

    @Override
    public Optional<OrdemDeServico> buscarPorNumero(String numeroOrdemServico) {
        return repository.findByNumeroOrdemServico(numeroOrdemServico).map(OrdemDeServicoJpaEntity::toDomain);
    }

    @Override
    public List<OrdemDeServico> buscarTodas() {
        return repository.findAll().stream().map(OrdemDeServicoJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void excluirPorNumero(String numeroOrdemServico) {
        repository.deleteByNumeroOrdemServico(numeroOrdemServico);
    }
}
