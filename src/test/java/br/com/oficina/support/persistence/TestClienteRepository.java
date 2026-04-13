package br.com.oficina.support.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

public class TestClienteRepository implements ClienteRepository {
    private final Map<String, Cliente> clientes = new ConcurrentHashMap<>();

    @Override
    public Cliente salvar(Cliente cliente) {
        Cliente clientePersistido = cliente.getId() == null
                ? new Cliente(UUID.randomUUID().toString(), cliente.getNome(), cliente.getCpfOuCnpj(), cliente.getTipoCliente())
                : cliente;
        clientes.put(clientePersistido.getId(), clientePersistido);
        return clientePersistido;
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
