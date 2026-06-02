package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.ordemservico.application.query.ConsultarFuncionarioQuery;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.support.persistence.TestFuncionarioRepository;

class ConsultarFuncionarioServiceTest {

    @Test
    void deveConsultarFuncionarioPorId() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        UUID funcionarioId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        repository.salvar(Funcionario.reconstituir(funcionarioId, "Joao", "12345678909"));
        ConsultarFuncionarioService service = new ConsultarFuncionarioService(repository);

        Funcionario encontrado = service.consultarFuncionario(new ConsultarFuncionarioQuery(funcionarioId)).orElseThrow();

        assertEquals("Joao", encontrado.getNome());
        assertEquals("12345678909", encontrado.getCpf());
    }

    @Test
    void deveRetornarVazioQuandoFuncionarioNaoExistir() {
        ConsultarFuncionarioService service = new ConsultarFuncionarioService(new TestFuncionarioRepository());

        assertTrue(service.consultarFuncionario(
                new ConsultarFuncionarioQuery(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))).isEmpty());
    }
}
