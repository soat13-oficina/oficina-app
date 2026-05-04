package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.RemoverEstoquePecaCommand;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class RemoverEstoquePecaServiceTest {

    @Test
    void deveRemoverEstoqueComSucesso() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-1", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        RemoverEstoquePecaService service = new RemoverEstoquePecaService(repository);

        service.removerEstoque(new RemoverEstoquePecaCommand("peca-1", 3));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-1").orElseThrow();
        assertEquals(7, pecaAtualizada.getQuantidadeEstoque());
    }

    @Test
    void deveRemoverTodoOEstoque() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-2", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 5, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        RemoverEstoquePecaService service = new RemoverEstoquePecaService(repository);

        service.removerEstoque(new RemoverEstoquePecaCommand("peca-2", 5));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-2").orElseThrow();
        assertEquals(0, pecaAtualizada.getQuantidadeEstoque());
    }

    @Test
    void deveFalharAoRemoverQuantidadeMaiorQueEstoque() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-3", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 5, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        RemoverEstoquePecaService service = new RemoverEstoquePecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.removerEstoque(new RemoverEstoquePecaCommand("peca-3", 10)));

        assertEquals("Estoque insuficiente. Quantidade atual: 5, quantidade solicitada: 10",
                exception.getMessage());
    }

    @Test
    void deveFalharAoRemoverQuantidadeZero() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-4", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        RemoverEstoquePecaService service = new RemoverEstoquePecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.removerEstoque(new RemoverEstoquePecaCommand("peca-4", 0)));

        assertEquals("A quantidade a ser removida deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoRemoverQuantidadeNegativa() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-5", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        RemoverEstoquePecaService service = new RemoverEstoquePecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.removerEstoque(new RemoverEstoquePecaCommand("peca-5", -3)));

        assertEquals("A quantidade a ser removida deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoRemoverEstoqueParaPecaInexistente() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        RemoverEstoquePecaService service = new RemoverEstoquePecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.removerEstoque(new RemoverEstoquePecaCommand("id-inexistente", 5)));

        assertEquals("Peça/Insumo não encontrada com o ID: id-inexistente", exception.getMessage());
    }
}
