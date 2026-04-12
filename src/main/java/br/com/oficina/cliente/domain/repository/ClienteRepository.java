package br.com.oficina.cliente.domain.repository;

import java.util.Optional;

import br.com.oficina.cliente.domain.model.Cliente;

public interface ClienteRepository {
    void salvar(Cliente cliente);

    void atualizar(Cliente cliente);

    void excluirPorId(String clienteId);

    Optional<Cliente> buscarPorId(String clienteId);
}
