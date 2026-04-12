package br.com.oficina.cliente.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;

class InMemoryClienteRepositoryTest {

    @Test
    void deveSalvarBuscarAtualizarEExcluirCliente() {
        InMemoryClienteRepository repository = new InMemoryClienteRepository();
        repository.salvar(new Cliente("cliente-1", "Maria", "12345678901"));

        assertEquals("Maria", repository.buscarPorId("cliente-1").orElseThrow().getNome());

        repository.atualizar(new Cliente("cliente-1", "Bianca", "99999999999"));
        assertEquals("Bianca", repository.buscarPorId("cliente-1").orElseThrow().getNome());

        repository.excluirPorId("cliente-1");
        assertTrue(repository.buscarPorId("cliente-1").isEmpty());
    }
}
