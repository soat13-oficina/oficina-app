package br.com.oficina.cliente.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

@Repository
@Transactional(readOnly = true)
public class JpaClienteRepository implements ClienteRepository {
    private final SpringDataClienteRepository repository;

    public JpaClienteRepository(SpringDataClienteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvar(Cliente cliente) {
        repository.save(ClienteJpaEntity.fromDomain(cliente));
    }

    @Override
    @Transactional
    public void atualizar(Cliente cliente) {
        repository.save(ClienteJpaEntity.fromDomain(cliente));
    }

    @Override
    @Transactional
    public void excluirPorId(String clienteId) {
        repository.deleteById(clienteId);
    }

    @Override
    public Optional<Cliente> buscarPorId(String clienteId) {
        return repository.findById(clienteId).map(ClienteJpaEntity::toDomain);
    }
}
