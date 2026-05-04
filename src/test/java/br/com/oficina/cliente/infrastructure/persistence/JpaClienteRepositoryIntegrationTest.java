package br.com.oficina.cliente.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class JpaClienteRepositoryIntegrationTest {

    @Autowired
    private JpaClienteRepository repository;

    @Test
    void devePersistirBuscarAtualizarEExcluirClienteNoBanco() {
        Cliente salvo = repository.salvar(new Cliente("Maria Silva", "123.456.789-01", TipoCliente.PF));

        assertTrue(repository.buscarPorId(salvo.getId()).isPresent());
        assertEquals(1, repository.buscarTodos().size());

        salvo.alterar("Maria Souza", "12.345.678/0001-99", TipoCliente.PJ);
        repository.atualizar(salvo);

        Cliente atualizado = repository.buscarPorId(salvo.getId()).orElseThrow();
        assertEquals("Maria Souza", atualizado.getNome());
        assertEquals("12.345.678/0001-99", atualizado.getCpfOuCnpj());
        assertEquals(TipoCliente.PJ, atualizado.getTipoCliente());

        repository.excluirPorId(salvo.getId());

        assertFalse(repository.buscarPorId(salvo.getId()).isPresent());
    }

    @Test
    void deveBuscarClientePorNomeEDocumentoIgnorandoFormatacao() {
        repository.salvar(new Cliente("Joao da Silva", "123.456.789-01", TipoCliente.PF));

        assertTrue(repository.buscarPorNomeEDocumento(" joao da silva ", "12345678901").isPresent());
        assertTrue(repository.buscarPorDocumento("12345678901").isPresent());
    }
}
