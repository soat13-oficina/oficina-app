package br.com.oficina.cliente.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;

/**
 * Valida o caminho de corrida da unicidade de documento (spec 008 / CRI-002, A1): a constraint UNIQUE
 * rejeita um documento duplicado inserido diretamente pelo repositório, contornando a verificação prévia do
 * service. Garante FR-004/SC-003 — não há como persistir dois clientes com o mesmo documento.
 */
@SpringBootTest
class ClienteUnicidadeDocumentoIntegrationTest {

    @Autowired
    private SpringDataClienteRepository clienteRepository;

    @BeforeEach
    void setUp() {
        clienteRepository.deleteAll();
    }

    @Test
    void deveRejeitarDocumentoDuplicadoPelaConstraintAoInserirDireto() {
        clienteRepository.saveAndFlush(new Cliente("Maria", "12345678901", TipoCliente.PF));

        assertThrows(DataIntegrityViolationException.class,
                () -> clienteRepository.saveAndFlush(new Cliente("Ana", "12345678901", TipoCliente.PF)));
    }

    @Test
    void devePermitirMultiplosClientesSemDocumento() {
        clienteRepository.saveAndFlush(new Cliente("Maria"));
        clienteRepository.saveAndFlush(new Cliente("Ana"));
    }
}
