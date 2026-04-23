package br.com.oficina.pecainsumo.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.pecainsumo.application.command.AdicionarEstoquePecaCommand;
import br.com.oficina.pecainsumo.application.command.AlterarPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.command.CadastrarPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.command.ExcluirPecaInsumoCommand;
import br.com.oficina.pecainsumo.application.command.LiberarReservaPecaCommand;
import br.com.oficina.pecainsumo.application.command.RemoverEstoquePecaCommand;
import br.com.oficina.pecainsumo.application.command.ReservarPecaCommand;
import br.com.oficina.pecainsumo.application.query.ListarPecasInsumosQuery;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class FluxoPecaInsumoServicesTest {

    @Test
    void deveCadastrarAlterarListarEExcluirPecaInsumo() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();

        new CadastrarPecaInsumoService(repository).cadastrarPecaInsumo(new CadastrarPecaInsumoCommand(
                "Filtro de ar",
                "Mann",
                new BigDecimal("45.90"),
                8,
                "FA-001",
                CategoriaPeca.FILTROS));

        PecaInsumo cadastrada = repository.buscarTodos().get(0);
        assertEquals("Filtro de ar", cadastrada.getDescricao());

        new AlterarPecaInsumoService(repository).alterarPecaInsumo(new AlterarPecaInsumoCommand(
                cadastrada.getId(),
                "Filtro de ar esportivo",
                "K&N",
                new BigDecimal("199.90"),
                6,
                1,
                "FA-002",
                CategoriaPeca.FILTROS));

        PecaInsumo alterada = repository.buscarPorId(cadastrada.getId()).orElseThrow();
        assertEquals("Filtro de ar esportivo", alterada.getDescricao());
        assertEquals("K&N", alterada.getMarca());

        List<PecaInsumo> filtradas = new ListarPecasInsumosService(repository)
                .listarPecasInsumos(new ListarPecasInsumosQuery("K&N", CategoriaPeca.FILTROS, true));
        assertEquals(1, filtradas.size());
        assertEquals(alterada.getId(), filtradas.get(0).getId());

        List<PecaInsumo> semReserva = new ListarPecasInsumosService(repository)
                .listarPecasInsumos(new ListarPecasInsumosQuery(null, null, false));
        assertTrue(semReserva.isEmpty());

        new ExcluirPecaInsumoService(repository).excluirPecaInsumo(new ExcluirPecaInsumoCommand(alterada.getId()));

        assertTrue(repository.buscarPorId(alterada.getId()).isEmpty());
    }

    @Test
    void deveAdicionarRemoverReservarELiberarQuantidadeDaPeca() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = novaPeca("peca-estoque", 10, 2);
        repository.salvar(peca);

        new AdicionarEstoquePecaService(repository)
                .adicionarEstoque(new AdicionarEstoquePecaCommand(peca.getId(), 5));
        assertEquals(15, repository.buscarPorId(peca.getId()).orElseThrow().getQuantidadeEstoque());

        new RemoverEstoquePecaService(repository)
                .removerEstoque(new RemoverEstoquePecaCommand(peca.getId(), 4));
        assertEquals(11, repository.buscarPorId(peca.getId()).orElseThrow().getQuantidadeEstoque());

        new ReservarPecaService(repository)
                .reservarPeca(new ReservarPecaCommand(peca.getId(), 3));
        assertEquals(5, repository.buscarPorId(peca.getId()).orElseThrow().getQuantidadeReservada());

        new LiberarReservaPecaService(repository)
                .liberarReserva(new LiberarReservaPecaCommand(peca.getId(), 2));
        assertEquals(3, repository.buscarPorId(peca.getId()).orElseThrow().getQuantidadeReservada());
    }

    @Test
    void deveFalharQuandoOperacaoReceberQuantidadeInvalidaOuPecaNaoExistir() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();

        RegraDeNegocioException adicionarInvalido = assertThrows(
                RegraDeNegocioException.class,
                () -> new AdicionarEstoquePecaService(repository)
                        .adicionarEstoque(new AdicionarEstoquePecaCommand("peca-404", 0)));
        assertEquals("A quantidade a ser adicionada deve ser maior que zero", adicionarInvalido.getMessage());

        RegraDeNegocioException removerInexistente = assertThrows(
                RegraDeNegocioException.class,
                () -> new RemoverEstoquePecaService(repository)
                        .removerEstoque(new RemoverEstoquePecaCommand("peca-404", 1)));
        assertEquals("Peça/Insumo não encontrada com o ID: peca-404", removerInexistente.getMessage());

        RegraDeNegocioException reservarInvalido = assertThrows(
                RegraDeNegocioException.class,
                () -> new ReservarPecaService(repository)
                        .reservarPeca(new ReservarPecaCommand("peca-404", -1)));
        assertEquals("A quantidade a ser reservada deve ser maior que zero", reservarInvalido.getMessage());

        RegraDeNegocioException liberarInvalido = assertThrows(
                RegraDeNegocioException.class,
                () -> new LiberarReservaPecaService(repository)
                        .liberarReserva(new LiberarReservaPecaCommand("peca-404", 0)));
        assertEquals("A quantidade a ser liberada deve ser maior que zero", liberarInvalido.getMessage());
    }

    @Test
    void deveFalharQuandoReservaOuRemocaoExcederemSaldoDisponivel() {
        TestPecaInsumoRepository repository = new TestPecaInsumoRepository();
        PecaInsumo peca = novaPeca("peca-limite", 5, 2);
        repository.salvar(peca);

        RegraDeNegocioException estoqueInsuficiente = assertThrows(
                RegraDeNegocioException.class,
                () -> new RemoverEstoquePecaService(repository)
                        .removerEstoque(new RemoverEstoquePecaCommand(peca.getId(), 6)));
        assertEquals("Estoque insuficiente. Quantidade atual: 5, quantidade solicitada: 6", estoqueInsuficiente.getMessage());

        RegraDeNegocioException reservaInsuficiente = assertThrows(
                RegraDeNegocioException.class,
                () -> new ReservarPecaService(repository)
                        .reservarPeca(new ReservarPecaCommand(peca.getId(), 4)));
        assertEquals("Quantidade insuficiente para reserva. Disponível: 3, solicitado: 4", reservaInsuficiente.getMessage());

        RegraDeNegocioException liberacaoInsuficiente = assertThrows(
                RegraDeNegocioException.class,
                () -> new LiberarReservaPecaService(repository)
                        .liberarReserva(new LiberarReservaPecaCommand(peca.getId(), 3)));
        assertEquals("Quantidade reservada insuficiente. Reservada atualmente: 2, solicitado liberar: 3", liberacaoInsuficiente.getMessage());
    }

    private PecaInsumo novaPeca(String id, int quantidadeEstoque, int quantidadeReservada) {
        return new PecaInsumo(
                id,
                "Pastilha de freio",
                "Cobreq",
                new BigDecimal("119.90"),
                quantidadeEstoque,
                quantidadeReservada,
                "PF-001",
                CategoriaPeca.FREIOS);
    }
}
