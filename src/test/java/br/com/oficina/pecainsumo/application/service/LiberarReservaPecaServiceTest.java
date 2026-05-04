package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.LiberarReservaPecaCommand;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class LiberarReservaPecaServiceTest {

    @Test
    void deveLiberarReservaComSucesso() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-1", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 5, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        LiberarReservaPecaService service = new LiberarReservaPecaService(repository);

        service.liberarReserva(new LiberarReservaPecaCommand("peca-1", 3));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-1").orElseThrow();
        assertEquals(10, pecaAtualizada.getQuantidadeEstoque());
        assertEquals(2, pecaAtualizada.getQuantidadeReservada());
        assertEquals(8, pecaAtualizada.getQuantidadeDisponivel());
    }

    @Test
    void deveLiberarTodaAReserva() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-2", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 5, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        LiberarReservaPecaService service = new LiberarReservaPecaService(repository);

        service.liberarReserva(new LiberarReservaPecaCommand("peca-2", 5));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-2").orElseThrow();
        assertEquals(0, pecaAtualizada.getQuantidadeReservada());
        assertEquals(10, pecaAtualizada.getQuantidadeDisponivel());
    }

    @Test
    void deveFalharAoLiberarMaisDoQueReservado() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-3", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 3, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        LiberarReservaPecaService service = new LiberarReservaPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.liberarReserva(new LiberarReservaPecaCommand("peca-3", 5)));

        assertEquals("Quantidade reservada insuficiente. Reservada atualmente: 3, solicitado liberar: 5",
                exception.getMessage());
    }

    @Test
    void deveFalharAoLiberarQuantidadeZero() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-4", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 5, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        LiberarReservaPecaService service = new LiberarReservaPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.liberarReserva(new LiberarReservaPecaCommand("peca-4", 0)));

        assertEquals("A quantidade a ser liberada deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoLiberarQuantidadeNegativa() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-5", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 5, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        LiberarReservaPecaService service = new LiberarReservaPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.liberarReserva(new LiberarReservaPecaCommand("peca-5", -1)));

        assertEquals("A quantidade a ser liberada deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoLiberarReservaParaPecaInexistente() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        LiberarReservaPecaService service = new LiberarReservaPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.liberarReserva(new LiberarReservaPecaCommand("id-inexistente", 5)));

        assertEquals("Peça/Insumo não encontrada com o ID: id-inexistente", exception.getMessage());
    }
}
