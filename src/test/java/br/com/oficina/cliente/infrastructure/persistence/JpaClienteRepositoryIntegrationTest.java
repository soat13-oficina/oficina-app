package br.com.oficina.cliente.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.support.PostgresIntegrationTest;

@Transactional
class JpaClienteRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JpaClienteRepository repository;

    @Test
    void devePersistirBuscarAtualizarEExcluirClienteNoBanco() {
        Cliente salvo = repository.salvar(new Cliente("Maria Silva", "123.456.789-09", TipoCliente.PF));

        assertTrue(repository.buscarPorId(salvo.getId()).isPresent());
        assertEquals(1, repository.buscarTodos().size());

        salvo.alterar("Maria Souza", "11.444.777/0001-61", TipoCliente.PJ);
        repository.atualizar(salvo);

        Cliente atualizado = repository.buscarPorId(salvo.getId()).orElseThrow();
        assertEquals("Maria Souza", atualizado.getNome());
        assertEquals("11.444.777/0001-61", atualizado.getCpfOuCnpj());
        assertEquals(TipoCliente.PJ, atualizado.getTipoCliente());

        repository.excluirPorId(salvo.getId());

        assertFalse(repository.buscarPorId(salvo.getId()).isPresent());
    }

    @Test
    void deveBuscarClientePorNomeEDocumentoIgnorandoFormatacao() {
        repository.salvar(new Cliente("Joao da Silva", "123.456.789-09", TipoCliente.PF));

        assertTrue(repository.buscarPorNomeEDocumento(" joao da silva ", "12345678909").isPresent());
        assertTrue(repository.buscarPorDocumento("12345678909").isPresent());
    }
}
