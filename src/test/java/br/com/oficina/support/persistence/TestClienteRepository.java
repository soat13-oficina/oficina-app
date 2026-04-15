package br.com.oficina.support.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;

public class TestClienteRepository implements ClienteRepository {
    private final Map<UUID, Cliente> clientes = new ConcurrentHashMap<>();

    @Override
    public Cliente salvar(Cliente cliente) {
        Cliente clientePersistido = cliente.getId() == null
                ? Cliente.reconstituir(UUID.randomUUID(), cliente.getNome(), cliente.getCpfOuCnpj(), cliente.getTipoCliente())
                : cliente;
        clientes.put(clientePersistido.getId(), clientePersistido);
        return clientePersistido;
    }

    @Override
    public void atualizar(Cliente cliente) {
        clientes.put(cliente.getId(), cliente);
    }

    @Override
    public void excluirPorId(UUID clienteId) {
        clientes.remove(clienteId);
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID clienteId) {
        return Optional.ofNullable(clientes.get(clienteId));
    }

    @Override
    public List<Cliente> buscarTodos() {
        return clientes.values().stream().toList();
    }

    @Override
    public Optional<Cliente> buscarPorNomeEDocumento(String nome, String cpfOuCnpj) {
        return clientes.values().stream()
                .filter(cliente -> cliente.getNome() != null && cliente.getNome().trim().equalsIgnoreCase(nome.trim()))
                .filter(cliente -> documentosSaoIguais(cliente.getCpfOuCnpj(), cpfOuCnpj))
                .findFirst();
    }

    @Override
    public Optional<Cliente> buscarPorDocumento(String cpfOuCnpj) {
        return clientes.values().stream()
                .filter(cliente -> documentosSaoIguais(cliente.getCpfOuCnpj(), cpfOuCnpj))
                .findFirst();
    }

    private boolean documentosSaoIguais(String documentoAtual, String documentoInformado) {
        if (documentoAtual == null || documentoInformado == null) {
            return false;
        }

        return documentoAtual.replaceAll("\\D", "").equals(documentoInformado.replaceAll("\\D", ""));
    }
}
