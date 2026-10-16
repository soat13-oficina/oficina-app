package br.com.oficina.pecainsumo.application.saga;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;
import br.com.oficina.pecainsumo.domain.model.ReservaPeca;
import br.com.oficina.support.mensageria.PublicadorMensagensEmMemoria;
import br.com.oficina.support.persistence.TestPecaInsumoRepository;
import br.com.oficina.support.persistence.TestReservaPecaRepository;

/** Passo ReservarPecas e compensacao LiberarReserva do lado do estoque (futura Execution). */
class ReservasPelaSagaTest {

    private static final UUID SAGA = UUID.randomUUID();
    private static final UUID ORCAMENTO = UUID.randomUUID();

    private final TestPecaInsumoRepository pecas = new TestPecaInsumoRepository();
    private final TestReservaPecaRepository reservas = new TestReservaPecaRepository();
    private final PublicadorMensagensEmMemoria publicador = new PublicadorMensagensEmMemoria();
    private final FabricaEnvelopes fabrica = PublicadorMensagensEmMemoria.fabrica();
    private final ReservasPelaSaga service = new ReservasPelaSaga(
            pecas, reservas, new EventosEstoque(publicador, fabrica), PublicadorMensagensEmMemoria.JSON);
    private final UUID ordem = UUID.randomUUID();

    private void peca(String id, int estoque, int reservada) {
        pecas.salvar(new PecaInsumo(id, "Peca " + id, "Bosch", new BigDecimal("10.00"), estoque, reservada, "REF-" + id, CategoriaPeca.FILTROS));
    }

    private Envelope reservar(MensagensEstoque.ItemPeca... itens) {
        return fabrica.novo(MensagensEstoque.RESERVAR_PECAS, SAGA, ordem, new MensagensEstoque.ReservarPecas(ORCAMENTO, List.of(itens)));
    }

    private Envelope liberar(String motivo) {
        return fabrica.novo(MensagensEstoque.LIBERAR_RESERVA, SAGA, ordem, new MensagensEstoque.LiberarReserva(motivo));
    }

    private static MensagensEstoque.ItemPeca item(String id, int quantidade) {
        return new MensagensEstoque.ItemPeca(id, quantidade);
    }

    @Test
    void reservaTudoSomandoAMesmaPecaERespondeAoComando() {
        peca("filtro", 5, 0);
        peca("oleo", 10, 2);
        Envelope comando = reservar(item("oleo", 4), item("filtro", 1), item("filtro", 2));

        service.reservar(comando);

        assertThat(pecas.buscarPorId("filtro").orElseThrow().getQuantidadeReservada()).isEqualTo(3);
        assertThat(pecas.buscarPorId("oleo").orElseThrow().getQuantidadeReservada()).isEqualTo(6);
        assertThat(reservas.buscarPorOrdemDeServico(ordem))
                .extracting(ReservaPeca::pecaInsumoId, ReservaPeca::quantidade, ReservaPeca::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("filtro", 3, ReservaPeca.Status.RESERVADA),
                        org.assertj.core.groups.Tuple.tuple("oleo", 4, ReservaPeca.Status.RESERVADA));
        Envelope resposta = publicador.unica();
        assertThat(publicador.publicadas().get(0).fila()).isEqualTo(Filas.OS_SAGA_REPLIES);
        assertThat(resposta.type()).isEqualTo(MensagensEstoque.PECAS_RESERVADAS);
        assertThat(resposta.causationId()).isEqualTo(comando.messageId());
        assertThat(resposta.sagaId()).isEqualTo(SAGA);
        assertThat(publicador.payload(resposta, MensagensEstoque.PecasReservadas.class))
                .isEqualTo(new MensagensEstoque.PecasReservadas(ORCAMENTO, List.of(item("filtro", 3), item("oleo", 4))));
    }

    @Test
    void comQualquerPecaFaltandoNadaEReservado() {
        peca("filtro", 5, 0);
        peca("oleo", 3, 2);

        service.reservar(reservar(item("filtro", 2), item("oleo", 2), item("sumiu", 1)));

        assertThat(pecas.buscarPorId("filtro").orElseThrow().getQuantidadeReservada()).as("tudo ou nada").isZero();
        assertThat(pecas.buscarPorId("oleo").orElseThrow().getQuantidadeReservada()).isEqualTo(2);
        assertThat(reservas.buscarPorOrdemDeServico(ordem)).isEmpty();
        Envelope resposta = publicador.unica();
        assertThat(resposta.type()).isEqualTo(MensagensEstoque.RESERVA_FALHOU);
        assertThat(publicador.payload(resposta, MensagensEstoque.ReservaFalhou.class)).isEqualTo(new MensagensEstoque.ReservaFalhou(
                ORCAMENTO, List.of(new MensagensEstoque.PecaFaltante("oleo", 2, 1), new MensagensEstoque.PecaFaltante("sumiu", 1, 0))));
    }

    @Test
    void comandoReenviadoNaoReservaDeNovoESoRepeteAResposta() {
        peca("filtro", 5, 0);
        service.reservar(reservar(item("filtro", 2)));
        publicador.limpar();

        Envelope reenvio = reservar(item("filtro", 2));
        service.reservar(reenvio);

        assertThat(pecas.buscarPorId("filtro").orElseThrow().getQuantidadeReservada()).isEqualTo(2);
        Envelope resposta = publicador.unica();
        assertThat(resposta.type()).isEqualTo(MensagensEstoque.PECAS_RESERVADAS);
        assertThat(resposta.causationId()).isEqualTo(reenvio.messageId());
        assertThat(publicador.payload(resposta, MensagensEstoque.PecasReservadas.class).itens()).containsExactly(item("filtro", 2));
    }

    @Test
    void ordemSemPecasConfirmaComListaVazia() {
        service.reservar(reservar());

        assertThat(publicador.payload(publicador.unica(), MensagensEstoque.PecasReservadas.class).itens()).isEmpty();
    }

    @Test
    void liberarDevolveAoDisponivelEConfirma() {
        peca("filtro", 5, 0);
        peca("oleo", 10, 0);
        service.reservar(reservar(item("filtro", 2), item("oleo", 1)));
        publicador.limpar();

        service.liberar(liberar("PAGAMENTO_RECUSADO"));

        assertThat(pecas.buscarPorId("filtro").orElseThrow().getQuantidadeReservada()).isZero();
        assertThat(pecas.buscarPorId("filtro").orElseThrow().getQuantidadeEstoque()).isEqualTo(5);
        assertThat(pecas.buscarPorId("oleo").orElseThrow().getQuantidadeReservada()).isZero();
        assertThat(reservas.buscarPorOrdemDeServico(ordem)).extracting(ReservaPeca::status)
                .containsOnly(ReservaPeca.Status.LIBERADA);
        Envelope resposta = publicador.unica();
        assertThat(resposta.type()).isEqualTo(MensagensEstoque.RESERVA_LIBERADA);
        assertThat(publicador.payload(resposta, MensagensEstoque.ReservaLiberada.class).itens())
                .containsExactly(item("filtro", 2), item("oleo", 1));
    }

    @Test
    void liberarDeNovoOuSemReservaConfirmaComListaVazia() {
        peca("filtro", 5, 0);
        service.reservar(reservar(item("filtro", 2)));
        service.liberar(liberar("x"));
        publicador.limpar();

        service.liberar(liberar("x"));

        assertThat(pecas.buscarPorId("filtro").orElseThrow().getQuantidadeReservada()).as("nao libera duas vezes").isZero();
        assertThat(publicador.payload(publicador.unica(), MensagensEstoque.ReservaLiberada.class).itens()).isEmpty();
    }

    @Test
    void reservaDePecaQueSaiuDoCadastroEhLiberadaMesmoAssim() {
        peca("filtro", 5, 0);
        service.reservar(reservar(item("filtro", 2)));
        pecas.excluirPorId("filtro");

        service.liberar(liberar("FALHA_EXECUCAO"));

        assertThat(reservas.buscarPorOrdemDeServico(ordem)).extracting(ReservaPeca::status)
                .containsExactly(ReservaPeca.Status.LIBERADA);
    }

    @Test
    void aposLiberarUmaNovaReservaPodeSerFeita() {
        peca("filtro", 5, 0);
        service.reservar(reservar(item("filtro", 2)));
        service.liberar(liberar("ReservarPecas sem resposta"));

        service.reservar(reservar(item("filtro", 3)));

        assertThat(pecas.buscarPorId("filtro").orElseThrow().getQuantidadeReservada()).isEqualTo(3);
        assertThat(reservas.buscarPorOrdemDeServico(ordem)).singleElement()
                .satisfies(r -> {
                    assertThat(r.status()).isEqualTo(ReservaPeca.Status.RESERVADA);
                    assertThat(r.quantidade()).isEqualTo(3);
                });
    }
}
