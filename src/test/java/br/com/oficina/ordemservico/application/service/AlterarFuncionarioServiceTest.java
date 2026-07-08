package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.AlterarFuncionarioCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.support.persistence.TestFuncionarioRepository;

class AlterarFuncionarioServiceTest {

    @Test
    void deveAlterarNomeECpfDoFuncionario() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        UUID funcionarioId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        repository.salvar(Funcionario.reconstituir(funcionarioId, "Joao", "12345678909"));
        AlterarFuncionarioService service = new AlterarFuncionarioService(repository);

        service.alterarFuncionario(new AlterarFuncionarioCommand(funcionarioId, "Joao Atualizado", "98765432100"));

        Funcionario atualizado = repository.buscarPorId(funcionarioId).orElseThrow();
        assertEquals("Joao Atualizado", atualizado.getNome());
        assertEquals("98765432100", atualizado.getCpf());
    }

    @Test
    void deveAlterarRemovindoCpfDoFuncionario() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        UUID funcionarioId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        repository.salvar(Funcionario.reconstituir(funcionarioId, "Maria", "12345678909"));
        AlterarFuncionarioService service = new AlterarFuncionarioService(repository);

        service.alterarFuncionario(new AlterarFuncionarioCommand(funcionarioId, "Maria", null));

        Funcionario atualizado = repository.buscarPorId(funcionarioId).orElseThrow();
        assertEquals("Maria", atualizado.getNome());
        assertNull(atualizado.getCpf());
    }

    @Test
    void devePermitirAlterarMantendsOPropriosCpf() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        UUID funcionarioId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        repository.salvar(Funcionario.reconstituir(funcionarioId, "Carlos", "12345678909"));
        AlterarFuncionarioService service = new AlterarFuncionarioService(repository);

        service.alterarFuncionario(new AlterarFuncionarioCommand(funcionarioId, "Carlos Souza", "12345678909"));

        Funcionario atualizado = repository.buscarPorId(funcionarioId).orElseThrow();
        assertEquals("Carlos Souza", atualizado.getNome());
        assertEquals("12345678909", atualizado.getCpf());
    }

    @Test
    void deveFalharAoAlterarFuncionarioInexistente() {
        AlterarFuncionarioService service = new AlterarFuncionarioService(new TestFuncionarioRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.alterarFuncionario(new AlterarFuncionarioCommand(
                        UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"), "Nome", null)));

        assertEquals("Funcionario nao encontrado para o identificador informado.", exception.getMessage());
    }

    @Test
    void deveFalharAoAlterarParaCpfJaUtilizadoPorOutroFuncionario() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        UUID funcionarioId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID outroFuncionarioId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        repository.salvar(Funcionario.reconstituir(funcionarioId, "Joao", "12345678909"));
        repository.salvar(Funcionario.reconstituir(outroFuncionarioId, "Maria", "52998224725"));
        AlterarFuncionarioService service = new AlterarFuncionarioService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.alterarFuncionario(new AlterarFuncionarioCommand(
                        outroFuncionarioId, "Maria", "123.456.789-09")));

        assertEquals("Ja existe funcionario cadastrado com o mesmo CPF.", exception.getMessage());
    }

    @Test
    void deveFalharAoAlterarParaNomeVazio() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        UUID funcionarioId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        repository.salvar(Funcionario.reconstituir(funcionarioId, "Ana", "12345678909"));
        AlterarFuncionarioService service = new AlterarFuncionarioService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.alterarFuncionario(new AlterarFuncionarioCommand(funcionarioId, " ", null)));

        assertEquals("Nome do funcionario e obrigatorio", exception.getMessage());
    }
}
