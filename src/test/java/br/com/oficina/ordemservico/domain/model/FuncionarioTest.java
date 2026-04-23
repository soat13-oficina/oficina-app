package br.com.oficina.ordemservico.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;

class FuncionarioTest {

    @Test
    void deveCriarFuncionarioComNomeECpfInformados() {
        Funcionario funcionario = new Funcionario("Joao", "12345678901");

        assertEquals("Joao", funcionario.getNome());
        assertEquals("12345678901", funcionario.getCpf());
    }

    @Test
    void deveCriarFuncionarioSemCpf() {
        Funcionario funcionario = new Funcionario("Maria", null);

        assertEquals("Maria", funcionario.getNome());
        assertNull(funcionario.getCpf());
    }

    @Test
    void deveCriarFuncionarioComCpfFormatado() {
        Funcionario funcionario = new Funcionario("Ana", "123.456.789-01");

        assertEquals("Ana", funcionario.getNome());
        assertEquals("123.456.789-01", funcionario.getCpf());
    }

    @Test
    void deveReconstituirFuncionarioComIdentificadorExistente() {
        UUID funcionarioId = UUID.fromString("51111111-1111-1111-1111-111111111111");

        Funcionario funcionario = Funcionario.reconstituir(funcionarioId, "Maria", null);

        assertNotNull(funcionario.getId());
        assertEquals(funcionarioId, funcionario.getId());
        assertEquals("Maria", funcionario.getNome());
        assertNull(funcionario.getCpf());
    }

    @Test
    void deveFalharQuandoNomeNaoForInformado() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new Funcionario(null, "12345678901"));

        assertEquals("Nome do funcionario e obrigatorio", exception.getMessage());
    }

    @Test
    void deveFalharQuandoNomeForVazio() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new Funcionario("   ", "12345678901"));

        assertEquals("Nome do funcionario e obrigatorio", exception.getMessage());
    }

    @Test
    void deveFalharQuandoCpfNaoPossuirOnzeDigitos() {
        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> new Funcionario("Carlos", "1234567890"));

        assertEquals("CPF do funcionario deve possuir 11 digitos", exception.getMessage());
    }

    @Test
    void deveAlterarNomeECpfDoFuncionario() {
        Funcionario funcionario = new Funcionario("Joao", "12345678901");

        funcionario.alterar("Joao Atualizado", "98765432100");

        assertEquals("Joao Atualizado", funcionario.getNome());
        assertEquals("98765432100", funcionario.getCpf());
    }

    @Test
    void deveAlterarRemovindoCpf() {
        Funcionario funcionario = new Funcionario("Joao", "12345678901");

        funcionario.alterar("Joao", null);

        assertEquals("Joao", funcionario.getNome());
        assertNull(funcionario.getCpf());
    }

    @Test
    void deveFalharAoAlterarQuandoNomeForVazio() {
        Funcionario funcionario = new Funcionario("Joao", "12345678901");

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> funcionario.alterar(" ", "12345678901"));

        assertEquals("Nome do funcionario e obrigatorio", exception.getMessage());
    }

    @Test
    void deveFalharAoAlterarQuandoCpfForInvalido() {
        Funcionario funcionario = new Funcionario("Joao", "12345678901");

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> funcionario.alterar("Joao", "123456"));

        assertEquals("CPF do funcionario deve possuir 11 digitos", exception.getMessage());
    }
}
