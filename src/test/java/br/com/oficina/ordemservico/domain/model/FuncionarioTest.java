package br.com.oficina.ordemservico.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class FuncionarioTest {

    @Test
    void deveCriarFuncionarioComNomeECpfInformados() {
        Funcionario funcionario = new Funcionario("Joao", "12345678901");

        assertEquals("Joao", funcionario.getNome());
        assertEquals("12345678901", funcionario.getCpf());
    }

    @Test
    void deveReconstituirFuncionarioComIdentificadorExistente() {
        UUID funcionarioId = UUID.fromString("51111111-1111-1111-1111-111111111111");

        Funcionario funcionario = Funcionario.reconstituir(funcionarioId, "Maria", null);

        assertNotNull(funcionario.getId());
        assertEquals(funcionarioId, funcionario.getId());
        assertEquals("Maria", funcionario.getNome());
        assertEquals(null, funcionario.getCpf());
    }
}
