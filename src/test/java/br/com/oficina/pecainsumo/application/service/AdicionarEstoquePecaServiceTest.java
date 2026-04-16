package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.AdicionarEstoquePecaCommand;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class AdicionarEstoquePecaServiceTest {

    @Test
    void deveAdicionarEstoqueComSucesso() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-1", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        AdicionarEstoquePecaService service = new AdicionarEstoquePecaService(repository);

        service.adicionarEstoque(new AdicionarEstoquePecaCommand("peca-1", 5));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-1").orElseThrow();
        assertEquals(15, pecaAtualizada.getQuantidadeEstoque());
    }

    @Test
    void deveFalharAoAdicionarQuantidadeZero() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-2", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        AdicionarEstoquePecaService service = new AdicionarEstoquePecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.adicionarEstoque(new AdicionarEstoquePecaCommand("peca-2", 0)));

        assertEquals("A quantidade a ser adicionada deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoAdicionarQuantidadeNegativa() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-3", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        AdicionarEstoquePecaService service = new AdicionarEstoquePecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.adicionarEstoque(new AdicionarEstoquePecaCommand("peca-3", -5)));

        assertEquals("A quantidade a ser adicionada deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoAdicionarEstoqueParaPecaInexistente() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        AdicionarEstoquePecaService service = new AdicionarEstoquePecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.adicionarEstoque(new AdicionarEstoquePecaCommand("id-inexistente", 5)));

        assertEquals("Peça/Insumo não encontrada com o ID: id-inexistente", exception.getMessage());
    }

    @Test
    void deveManterDemaisCamposAoAdicionarEstoque() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-4", "Pastilha de freio", "TRW",
                new BigDecimal("120.00"), 20, 3, "TRW001", CategoriaPeca.FREIOS);
        repository.salvar(peca);
        AdicionarEstoquePecaService service = new AdicionarEstoquePecaService(repository);

        service.adicionarEstoque(new AdicionarEstoquePecaCommand("peca-4", 10));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-4").orElseThrow();
        assertEquals(30, pecaAtualizada.getQuantidadeEstoque());
        assertEquals(3, pecaAtualizada.getQuantidadeReservada());
        assertEquals("Pastilha de freio", pecaAtualizada.getDescricao());
        assertEquals("TRW", pecaAtualizada.getMarca());
        assertEquals(new BigDecimal("120.00"), pecaAtualizada.getPreco());
        assertEquals(CategoriaPeca.FREIOS, pecaAtualizada.getCategoria());
    }
}
