package br.com.oficina.orcamento.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.AprovarOrcamentoCommand;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.PecaOrcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.pecainsumo.application.service.LiberarReservaPecaService;
import br.com.oficina.pecainsumo.application.service.ReservarPecaService;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.support.persistence.TestOrcamentoRepository;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;

class AprovarOrcamentoServiceTest {

    private static final String PECA_COM_ESTOQUE = "peca-disco-001";
    private static final String PECA_SEM_ESTOQUE = "peca-pastilha-001";

    @Test
    void deveAprovarOrcamentoReservandoTodasAsPecas() {
        TestPecaInsumoRepository pecaInsumoRepository = new TestPecaInsumoRepository();
        TestOrcamentoRepository orcamentoRepository = new TestOrcamentoRepository();
        pecaInsumoRepository.salvar(peca(PECA_COM_ESTOQUE, 10, 0));
        pecaInsumoRepository.salvar(peca(PECA_SEM_ESTOQUE, 10, 0));
        orcamentoRepository.salvar(orcamentoAguardandoAprovacao());

        AprovarOrcamentoService service = new AprovarOrcamentoService(
                orcamentoRepository,
                new ReservarPecaService(pecaInsumoRepository),
                new LiberarReservaPecaService(pecaInsumoRepository));

        Orcamento aprovado = service.aprovarOrcamento(new AprovarOrcamentoCommand("ORC-1"));

        assertEquals(StatusOrcamento.APROVADO, aprovado.getStatus());
        assertEquals(2, pecaInsumoRepository.buscarPorId(PECA_COM_ESTOQUE).orElseThrow().getQuantidadeReservada());
        assertEquals(5, pecaInsumoRepository.buscarPorId(PECA_SEM_ESTOQUE).orElseThrow().getQuantidadeReservada());
    }

    @Test
    void deveLiberarReservasJaFeitasQuandoFaltarEstoqueParaAlgumaPeca() {
        TestPecaInsumoRepository pecaInsumoRepository = new TestPecaInsumoRepository();
        TestOrcamentoRepository orcamentoRepository = new TestOrcamentoRepository();
        // Primeira peca tem estoque suficiente; a segunda nao (estoque 1, pedido 5).
        pecaInsumoRepository.salvar(peca(PECA_COM_ESTOQUE, 10, 0));
        pecaInsumoRepository.salvar(peca(PECA_SEM_ESTOQUE, 1, 0));
        orcamentoRepository.salvar(orcamentoAguardandoAprovacao());

        AprovarOrcamentoService service = new AprovarOrcamentoService(
                orcamentoRepository,
                new ReservarPecaService(pecaInsumoRepository),
                new LiberarReservaPecaService(pecaInsumoRepository));

        RegraDeNegocioException exception = assertThrows(
                RegraDeNegocioException.class,
                () -> service.aprovarOrcamento(new AprovarOrcamentoCommand("ORC-1")));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("Estoque insuficiente"));
        // A reserva ja feita para a primeira peca deve ser desfeita (volta a 0), e nao ficar presa.
        assertEquals(0, pecaInsumoRepository.buscarPorId(PECA_COM_ESTOQUE).orElseThrow().getQuantidadeReservada());
        assertEquals(0, pecaInsumoRepository.buscarPorId(PECA_SEM_ESTOQUE).orElseThrow().getQuantidadeReservada());
    }

    private static PecaInsumo peca(String id, int estoque, int reservada) {
        return new PecaInsumo(id, "Peca " + id, "Bosch", new BigDecimal("100.00"), estoque, reservada, "REF-" + id, CategoriaPeca.FREIOS);
    }

    private static Orcamento orcamentoAguardandoAprovacao() {
        return new Orcamento(
                "ORC-1",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Maria Silva",
                "12345678909",
                "ABC1D23",
                "Toyota",
                "Corolla",
                "Troca de freios",
                List.of("Troca de disco"),
                List.of(
                        new PecaOrcamento(PECA_COM_ESTOQUE, "Disco de freio", new BigDecimal("350.00"), 2),
                        new PecaOrcamento(PECA_SEM_ESTOQUE, "Pastilha", new BigDecimal("180.00"), 5)),
                new BigDecimal("420.00"),
                BigDecimal.ZERO,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 8, 10, 0),
                "Cliente aguarda aprovacao",
                StatusOrcamento.AGUARDANDO_APROVACAO);
    }
}
