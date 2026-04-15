package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.ConsumirPecaCommand;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class ConsumirPecaServiceTest {

    @Test
    void deveConsumirPecaReservadaComSucesso() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-1", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 5, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ConsumirPecaService service = new ConsumirPecaService(repository);

        service.consumirPeca(new ConsumirPecaCommand("peca-1", 3));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-1").orElseThrow();
        assertEquals(7, pecaAtualizada.getQuantidadeEstoque());
        assertEquals(2, pecaAtualizada.getQuantidadeReservada());
        assertEquals(5, pecaAtualizada.getQuantidadeDisponivel());
    }

    @Test
    void deveConsumirTodaAQuantidadeReservada() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-2", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 5, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ConsumirPecaService service = new ConsumirPecaService(repository);

        service.consumirPeca(new ConsumirPecaCommand("peca-2", 5));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-2").orElseThrow();
        assertEquals(5, pecaAtualizada.getQuantidadeEstoque());
        assertEquals(0, pecaAtualizada.getQuantidadeReservada());
        assertEquals(5, pecaAtualizada.getQuantidadeDisponivel());
    }

    @Test
    void deveFalharAoConsumirMaisDoQueReservado() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-3", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 3, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ConsumirPecaService service = new ConsumirPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.consumirPeca(new ConsumirPecaCommand("peca-3", 5)));

        assertEquals("Quantidade reservada insuficiente para consumo. Reservada atualmente: 3, solicitado consumir: 5",
                exception.getMessage());
    }

    @Test
    void deveFalharAoConsumirSemNenhumaReserva() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-4", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ConsumirPecaService service = new ConsumirPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.consumirPeca(new ConsumirPecaCommand("peca-4", 2)));

        assertEquals("Quantidade reservada insuficiente para consumo. Reservada atualmente: 0, solicitado consumir: 2",
                exception.getMessage());
    }

    @Test
    void deveFalharAoConsumirQuantidadeZero() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-5", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 5, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ConsumirPecaService service = new ConsumirPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.consumirPeca(new ConsumirPecaCommand("peca-5", 0)));

        assertEquals("A quantidade a ser consumida deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoConsumirQuantidadeNegativa() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-6", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 5, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ConsumirPecaService service = new ConsumirPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.consumirPeca(new ConsumirPecaCommand("peca-6", -3)));

        assertEquals("A quantidade a ser consumida deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoConsumirPecaInexistente() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        ConsumirPecaService service = new ConsumirPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.consumirPeca(new ConsumirPecaCommand("id-inexistente", 5)));

        assertEquals("Peça/Insumo não encontrada com o ID: id-inexistente", exception.getMessage());
    }
}
