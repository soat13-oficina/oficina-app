package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.CadastrarFuncionarioCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.support.persistence.TestFuncionarioRepository;

class CadastrarFuncionarioServiceTest {

    @Test
    void deveCadastrarFuncionarioComNomeECpf() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        CadastrarFuncionarioService service = new CadastrarFuncionarioService(repository);

        UUID funcionarioId = service.cadastrarFuncionario(new CadastrarFuncionarioCommand("Joao Silva", "12345678909"));

        assertNotNull(funcionarioId);
        Funcionario salvo = repository.buscarPorId(funcionarioId).orElseThrow();
        assertEquals("Joao Silva", salvo.getNome());
        assertEquals("12345678909", salvo.getCpf());
    }

    @Test
    void deveCadastrarFuncionarioSemCpf() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        CadastrarFuncionarioService service = new CadastrarFuncionarioService(repository);

        UUID funcionarioId = service.cadastrarFuncionario(new CadastrarFuncionarioCommand("Maria Oliveira", null));

        assertNotNull(funcionarioId);
        Funcionario salvo = repository.buscarPorId(funcionarioId).orElseThrow();
        assertEquals("Maria Oliveira", salvo.getNome());
        assertNull(salvo.getCpf());
    }

    @Test
    void deveFalharAoCadastrarSemNome() {
        CadastrarFuncionarioService service = new CadastrarFuncionarioService(new TestFuncionarioRepository());

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarFuncionario(new CadastrarFuncionarioCommand(" ", "12345678909")));

        assertEquals("Nome do funcionario e obrigatorio", exception.getMessage());
    }

    @Test
    void deveFalharAoCadastrarComCpfInvalido() {
        CadastrarFuncionarioService service = new CadastrarFuncionarioService(new TestFuncionarioRepository());

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarFuncionario(new CadastrarFuncionarioCommand("Ana", "1234567890")));

        assertEquals("CPF do funcionario deve possuir 11 digitos", exception.getMessage());
    }

    @Test
    void deveFalharAoCadastrarComCpfJaUtilizadoPorOutroFuncionario() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Joao Existente", "12345678909"));
        CadastrarFuncionarioService service = new CadastrarFuncionarioService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.cadastrarFuncionario(new CadastrarFuncionarioCommand("Carlos Novo", "123.456.789-09")));

        assertEquals("Ja existe funcionario cadastrado com o mesmo CPF.", exception.getMessage());
    }

    @Test
    void devePermitirCadastrarDoisFuncionariosSemCpf() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        CadastrarFuncionarioService service = new CadastrarFuncionarioService(repository);

        UUID id1 = service.cadastrarFuncionario(new CadastrarFuncionarioCommand("Funcionario Um", null));
        UUID id2 = service.cadastrarFuncionario(new CadastrarFuncionarioCommand("Funcionario Dois", null));

        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals(2, repository.buscarTodos().size());
    }
}
