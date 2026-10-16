package br.com.oficina.pecainsumo.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.model.ReservaPeca;
import br.com.oficina.ponte.ReservasDaOrdemNoEstoque;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;
import br.com.oficina.support.persistence.TestReservaPecaRepository;

/** Conclusao do servico: o que a Saga reservou para a OS sai do estoque. */
class ConsumirReservasDaOrdemServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 1, 5, 10, 0);

    private final TestPecaInsumoRepository pecas = new TestPecaInsumoRepository();
    private final TestReservaPecaRepository reservas = new TestReservaPecaRepository();
    private final ConsumirReservasDaOrdemService service = new ConsumirReservasDaOrdemService(pecas, reservas);
    private final UUID ordem = UUID.randomUUID();

    private void peca(String id, int estoque, int reservada) {
        pecas.salvar(new PecaInsumo(id, "Peca " + id, "Bosch", new BigDecimal("10.00"), estoque, reservada, "REF-" + id, CategoriaPeca.FILTROS));
    }

    @Test
    void baixaAsReservasAtivasDaOrdemEMarcaComoConsumidas() {
        peca("filtro", 10, 3);
        peca("oleo", 8, 4);
        reservas.salvar(ReservaPeca.nova(ordem, "filtro", 2, AGORA));
        reservas.salvar(ReservaPeca.nova(ordem, "oleo", 4, AGORA));
        reservas.salvar(ReservaPeca.nova(UUID.randomUUID(), "filtro", 1, AGORA));

        int consumidas = new ReservasDaOrdemNoEstoque(service).consumir(ordem);

        assertThat(consumidas).isEqualTo(2);
        PecaInsumo filtro = pecas.buscarPorId("filtro").orElseThrow();
        assertThat(filtro.getQuantidadeEstoque()).isEqualTo(8);
        assertThat(filtro.getQuantidadeReservada()).as("a reserva da outra OS continua").isEqualTo(1);
        PecaInsumo oleo = pecas.buscarPorId("oleo").orElseThrow();
        assertThat(oleo.getQuantidadeEstoque()).isEqualTo(4);
        assertThat(oleo.getQuantidadeReservada()).isZero();
        assertThat(reservas.buscarPorOrdemDeServico(ordem)).extracting(ReservaPeca::status)
                .containsOnly(ReservaPeca.Status.CONSUMIDA);
    }

    @Test
    void reservasLiberadasOuJaConsumidasNaoSaemDeNovo() {
        peca("filtro", 10, 0);
        reservas.salvar(ReservaPeca.nova(ordem, "filtro", 2, AGORA).com(ReservaPeca.Status.LIBERADA, AGORA));

        assertThat(service.consumirReservas(ordem)).isZero();
        assertThat(pecas.buscarPorId("filtro").orElseThrow().getQuantidadeEstoque()).isEqualTo(10);
    }

    @Test
    void ordemSemReservaNaoMexeNoEstoque() {
        assertThat(service.consumirReservas(ordem)).isZero();
    }

    @Test
    void pecaQueSaiuDoCadastroSoTemAReservaEncerrada() {
        reservas.salvar(ReservaPeca.nova(ordem, "sumiu", 1, AGORA));

        assertThat(service.consumirReservas(ordem)).isEqualTo(1);
        assertThat(reservas.buscarPorOrdemDeServico(ordem)).singleElement()
                .satisfies(r -> {
                    assertThat(r.status()).isEqualTo(ReservaPeca.Status.CONSUMIDA);
                    assertThat(r.atualizadaEm()).isAfter(AGORA);
                });
    }
}
