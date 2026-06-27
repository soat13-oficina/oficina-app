package br.com.oficina.cliente.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
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
    public Cliente salvar(Cliente cliente) {
        return repository.save(cliente);
    }

    @Override
    @Transactional
    public void atualizar(Cliente cliente) {
        repository.save(cliente);
    }

    @Override
    @Transactional
    public void excluirPorId(UUID clienteId) {
        repository.deleteById(clienteId);
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID clienteId) {
        return repository.findById(clienteId);
    }

    @Override
    public List<Cliente> buscarTodos() {
        return repository.findAll();
    }

    @Override
    public Optional<Cliente> buscarPorNomeEDocumento(String nome, String cpfOuCnpj) {
        return repository.findAll().stream()
                .filter(cliente -> cliente.getNome() != null && cliente.getNome().trim().equalsIgnoreCase(nome.trim()))
                .filter(cliente -> documentosSaoIguais(cliente.getCpfOuCnpj(), cpfOuCnpj))
                .findFirst();
    }

    @Override
    public Optional<Cliente> buscarPorDocumento(String cpfOuCnpj) {
        if (cpfOuCnpj == null) {
            return Optional.empty();
        }
        return repository.findByCpfOuCnpj(cpfOuCnpj);
    }

    private boolean documentosSaoIguais(String documentoAtual, String documentoInformado) {
        if (documentoAtual == null || documentoInformado == null) {
            return false;
        }

        return documentoAtual.replaceAll("\\D", "").equals(documentoInformado.replaceAll("\\D", ""));
    }
}
