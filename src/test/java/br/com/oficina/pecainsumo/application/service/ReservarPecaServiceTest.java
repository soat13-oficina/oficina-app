package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.ReservarPecaCommand;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class ReservarPecaServiceTest {

    @Test
    void deveReservarPecaComSucesso() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-1", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ReservarPecaService service = new ReservarPecaService(repository);

        service.reservarPeca(new ReservarPecaCommand("peca-1", 3));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-1").orElseThrow();
        assertEquals(10, pecaAtualizada.getQuantidadeEstoque());
        assertEquals(3, pecaAtualizada.getQuantidadeReservada());
        assertEquals(7, pecaAtualizada.getQuantidadeDisponivel());
    }

    @Test
    void deveReservarTodoEstoqueDisponivel() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-2", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ReservarPecaService service = new ReservarPecaService(repository);

        service.reservarPeca(new ReservarPecaCommand("peca-2", 10));

        PecaInsumo pecaAtualizada = repository.buscarPorId("peca-2").orElseThrow();
        assertEquals(10, pecaAtualizada.getQuantidadeReservada());
        assertEquals(0, pecaAtualizada.getQuantidadeDisponivel());
    }

    @Test
    void deveFalharAoReservarMaisDoQueDisponivel() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-3", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 5, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ReservarPecaService service = new ReservarPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.reservarPeca(new ReservarPecaCommand("peca-3", 6)));

        assertEquals("Quantidade insuficiente para reserva. Disponível: 5, solicitado: 6",
                exception.getMessage());
    }

    @Test
    void deveFalharAoReservarQuantidadeZero() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-4", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ReservarPecaService service = new ReservarPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.reservarPeca(new ReservarPecaCommand("peca-4", 0)));

        assertEquals("A quantidade a ser reservada deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoReservarQuantidadeNegativa() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = new PecaInsumo("peca-5", "Filtro de óleo", "Bosch",
                new BigDecimal("45.90"), 10, 0, "OB001", CategoriaPeca.FILTROS);
        repository.salvar(peca);
        ReservarPecaService service = new ReservarPecaService(repository);

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.reservarPeca(new ReservarPecaCommand("peca-5", -2)));

        assertEquals("A quantidade a ser reservada deve ser maior que zero", exception.getMessage());
    }

    @Test
    void deveFalharAoReservarPecaInexistente() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        ReservarPecaService service = new ReservarPecaService(repository);

        RecursoNaoEncontradoException exception = assertThrows(
                RecursoNaoEncontradoException.class,
                () -> service.reservarPeca(new ReservarPecaCommand("id-inexistente", 5)));

        assertEquals("Peça/Insumo não encontrada com o ID: id-inexistente", exception.getMessage());
    }
}
