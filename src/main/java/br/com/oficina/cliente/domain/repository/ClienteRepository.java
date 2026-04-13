package br.com.oficina.cliente.domain.repository;

import java.util.UUID;
import java.util.Optional;

import br.com.oficina.cliente.domain.model.Cliente;

public interface ClienteRepository {
    Cliente salvar(Cliente cliente);

    void atualizar(Cliente cliente);

    void excluirPorId(UUID clienteId);

    Optional<Cliente> buscarPorId(UUID clienteId);
}
