package br.com.oficina.infrastructure.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import br.com.oficina.domain.model.cliente.Cliente;
import br.com.oficina.domain.repository.ClienteRepository;

@Repository
public class InMemoryClienteRepository implements ClienteRepository {
    private final Map<String, Cliente> clientes = new ConcurrentHashMap<>();

    @Override
    public void salvar(Cliente cliente) {
        clientes.put(cliente.getId(), cliente);
    }

    @Override
    public void atualizar(Cliente cliente) {
        clientes.put(cliente.getId(), cliente);
    }

    @Override
    public void excluirPorId(String clienteId) {
        clientes.remove(clienteId);
    }

    @Override
    public Optional<Cliente> buscarPorId(String clienteId) {
        return Optional.ofNullable(clientes.get(clienteId));
    }
}
