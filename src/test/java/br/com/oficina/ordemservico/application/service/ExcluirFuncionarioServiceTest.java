package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.ordemservico.application.command.ExcluirFuncionarioCommand;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.support.persistence.TestFuncionarioRepository;

class ExcluirFuncionarioServiceTest {

    @Test
    void deveExcluirFuncionarioExistente() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        UUID funcionarioId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        repository.salvar(Funcionario.reconstituir(funcionarioId, "Joao", "12345678901"));
        ExcluirFuncionarioService service = new ExcluirFuncionarioService(repository);

        service.excluirFuncionario(new ExcluirFuncionarioCommand(funcionarioId));

        assertTrue(repository.buscarPorId(funcionarioId).isEmpty());
    }

    @Test
    void deveFalharAoExcluirFuncionarioInexistente() {
        ExcluirFuncionarioService service = new ExcluirFuncionarioService(new TestFuncionarioRepository());

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.excluirFuncionario(new ExcluirFuncionarioCommand(
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))));

        assertEquals("Funcionario nao encontrado para o identificador informado.", exception.getMessage());
    }
}
