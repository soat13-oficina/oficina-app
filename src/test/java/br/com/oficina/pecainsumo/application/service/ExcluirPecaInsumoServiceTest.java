package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.ExcluirPecaInsumoCommand;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class ExcluirPecaInsumoServiceTest {

    @Test
    void deveExcluirPecaInsumoExistente() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo(
                "peca-excluir-1",
                "Filtro de óleo",
                "Bosch",
                new BigDecimal("45.90"),
                10,
                0,
                "OB0986B01044",
                CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ExcluirPecaInsumoService service = new ExcluirPecaInsumoService(repository);

        service.excluirPecaInsumo(new ExcluirPecaInsumoCommand("peca-excluir-1"));

        assertTrue(repository.buscarPorId("peca-excluir-1").isEmpty());
    }

    @Test
    void deveFalharAoExcluirPecaInsumoInexistente() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        ExcluirPecaInsumoService service = new ExcluirPecaInsumoService(repository);

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.excluirPecaInsumo(new ExcluirPecaInsumoCommand("id-inexistente")));

        assertEquals("Peça/Insumo não encontrada com o ID: id-inexistente", exception.getMessage());
        assertTrue(repository.buscarTodos().isEmpty());
    }

    @Test
    void deveFalharAoExcluirPecaInsumoComReservaAtiva() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo(
                "peca-reservada-1",
                "Filtro de óleo",
                "Bosch",
                new BigDecimal("45.90"),
                10,
                1,
                "OB0986B01044",
                CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ExcluirPecaInsumoService service = new ExcluirPecaInsumoService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.excluirPecaInsumo(new ExcluirPecaInsumoCommand("peca-reservada-1")));

        assertEquals("Não é possível excluir peça/insumo com reserva ativa.", exception.getMessage());
        assertTrue(repository.buscarPorId("peca-reservada-1").isPresent());
    }
}
