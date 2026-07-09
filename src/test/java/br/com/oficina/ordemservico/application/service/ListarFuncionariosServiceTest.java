package br.com.oficina.ordemservico.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.ordemservico.application.query.ListarFuncionariosQuery;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.support.persistence.TestFuncionarioRepository;

class ListarFuncionariosServiceTest {

    @Test
    void deveListarTodosOsFuncionariosOrdenadosPorNomeQuandoFiltroNaoForInformado() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Zé Último", null));
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Ana Primeira", null));
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Marcos Meio", null));
        ListarFuncionariosService service = new ListarFuncionariosService(repository);

        List<Funcionario> resultado = service.listarFuncionarios(new ListarFuncionariosQuery(null));

        assertEquals(3, resultado.size());
        assertEquals("Ana Primeira", resultado.get(0).getNome());
        assertEquals("Marcos Meio", resultado.get(1).getNome());
        assertEquals("Zé Último", resultado.get(2).getNome());
    }

    @Test
    void deveListarFuncionariosFiltradosPorNomeParcial() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Joao Silva", "12345678909"));
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Maria Souza", null));
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Joao Pereira", null));
        ListarFuncionariosService service = new ListarFuncionariosService(repository);

        List<Funcionario> resultado = service.listarFuncionarios(new ListarFuncionariosQuery("Joao"));

        assertEquals(2, resultado.size());
        assertEquals("Joao Pereira", resultado.get(0).getNome());
        assertEquals("Joao Silva", resultado.get(1).getNome());
    }

    @Test
    void deveIgnorarAcentuacaoEMaiusculasNoFiltro() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "José António", null));
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Carlos Lima", null));
        ListarFuncionariosService service = new ListarFuncionariosService(repository);

        List<Funcionario> resultado = service.listarFuncionarios(new ListarFuncionariosQuery("jose antonio"));

        assertEquals(1, resultado.size());
        assertEquals("José António", resultado.get(0).getNome());
    }

    @Test
    void deveRetornarListaVaziaQuandoFiltroNaoCorrespondaANenhumFuncionario() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Ana Lima", null));
        ListarFuncionariosService service = new ListarFuncionariosService(repository);

        List<Funcionario> resultado = service.listarFuncionarios(new ListarFuncionariosQuery("Inexistente"));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverFuncionariosCadastrados() {
        ListarFuncionariosService service = new ListarFuncionariosService(new TestFuncionarioRepository());

        List<Funcionario> resultado = service.listarFuncionarios(new ListarFuncionariosQuery(null));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveListarTodosQuandoFiltroForVazio() {
        TestFuncionarioRepository repository = new TestFuncionarioRepository();
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Ana", null));
        repository.salvar(Funcionario.reconstituir(UUID.randomUUID(), "Bruno", null));
        ListarFuncionariosService service = new ListarFuncionariosService(repository);

        List<Funcionario> resultado = service.listarFuncionarios(new ListarFuncionariosQuery("  "));

        assertEquals(2, resultado.size());
    }
}
