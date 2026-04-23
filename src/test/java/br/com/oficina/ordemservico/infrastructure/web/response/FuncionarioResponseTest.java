package br.com.oficina.ordemservico.infrastructure.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.ordemservico.domain.model.Funcionario;

class FuncionarioResponseTest {

    @Test
    void deveConverterFuncionarioComCpfParaResponse() {
        UUID funcionarioId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Funcionario funcionario = Funcionario.reconstituir(funcionarioId, "Joao Silva", "12345678901");

        FuncionarioResponse response = FuncionarioResponse.from(funcionario);

        assertEquals(funcionarioId, response.id());
        assertEquals("Joao Silva", response.nome());
        assertEquals("12345678901", response.cpf());
    }

    @Test
    void deveConverterFuncionarioSemCpfParaResponse() {
        UUID funcionarioId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Funcionario funcionario = Funcionario.reconstituir(funcionarioId, "Maria Souza", null);

        FuncionarioResponse response = FuncionarioResponse.from(funcionario);

        assertEquals(funcionarioId, response.id());
        assertEquals("Maria Souza", response.nome());
        assertNull(response.cpf());
    }
}
