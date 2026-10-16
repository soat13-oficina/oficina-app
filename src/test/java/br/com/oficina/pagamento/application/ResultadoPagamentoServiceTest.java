package br.com.oficina.pagamento.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.application.mensageria.Envelope;
import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.common.domain.exception.ConflitoDeRecursoException;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.pagamento.application.ResultadoPagamentoService.Resultado;
import br.com.oficina.pagamento.application.saga.EventosPagamento;
import br.com.oficina.pagamento.application.saga.MensagensPagamento;
import br.com.oficina.pagamento.domain.model.Pagamento;
import br.com.oficina.pagamento.domain.model.StatusPagamento;
import br.com.oficina.support.mensageria.PublicadorMensagensEmMemoria;
import br.com.oficina.support.persistence.TestOrcamentoRepository;
import br.com.oficina.support.persistence.TestPagamentoRepository;

/** Resultado da cobranca (simulacao hoje; webhook do Mercado Pago no Passo 3) e expiracao pelo prazo. */
class ResultadoPagamentoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.now();

    private final TestPagamentoRepository pagamentos = new TestPagamentoRepository();
    private final TestOrcamentoRepository orcamentos = new TestOrcamentoRepository();
    private final PublicadorMensagensEmMemoria publicador = new PublicadorMensagensEmMemoria();
    private final ResultadoPagamentoService service = new ResultadoPagamentoService(
            pagamentos, orcamentos, new EventosPagamento(publicador, PublicadorMensagensEmMemoria.fabrica()));
    private final CancelamentoDeCobrancas cancelamento = new CancelamentoDeCobrancas(pagamentos);
    private final UUID saga = UUID.randomUUID();
    private final UUID ordem = UUID.randomUUID();

    private Pagamento pendente(UUID orcamentoId, LocalDateTime expiraEm, LocalDateTime criadoEm) {
        return pagamentos.salvar(Pagamento.novo(orcamentoId, ordem, saga, new BigDecimal("360"), expiraEm, criadoEm));
    }

    private Pagamento pendente() {
        return pendente(UUID.randomUUID(), AGORA.plusDays(1), AGORA);
    }

    @Test
    void aprovacaoPublicaPagamentoConfirmadoParaASaga() {
        Pagamento pagamento = pendente();

        Pagamento registrado = service.registrar(pagamento.getId(), Resultado.APROVADO, "PAY-1", null);

        assertThat(registrado.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(publicador.publicadas().get(0).fila()).isEqualTo(Filas.OS_SAGA_REPLIES);
        Envelope evento = publicador.unica();
        assertThat(evento.type()).isEqualTo(MensagensPagamento.PAGAMENTO_CONFIRMADO);
        assertThat(evento.sagaId()).isEqualTo(saga);
        assertThat(evento.ordemServicoId()).isEqualTo(ordem);
        assertThat(evento.causationId()).as("evento espontaneo").isNull();
        assertThat(evento.payload().get("valor").asString()).isEqualTo("360.00");
        assertThat(publicador.payload(evento, MensagensPagamento.PagamentoConfirmado.class)).satisfies(confirmado -> {
            assertThat(confirmado.pagamentoId()).isEqualTo(pagamento.getId());
            assertThat(confirmado.mpPaymentId()).isEqualTo("PAY-1");
            assertThat(confirmado.aprovadoEm()).isNotNull();
        });
    }

    @Test
    void resultadoRepetidoNaoPublicaDeNovoEResultadoDiferenteEhConflito() {
        Pagamento pagamento = pendente();
        service.registrar(pagamento.getId(), Resultado.APROVADO, "PAY-1", null);
        publicador.limpar();

        service.registrar(pagamento.getId(), Resultado.APROVADO, "PAY-1", null);

        assertThat(publicador.publicadas()).isEmpty();
        assertThatThrownBy(() -> service.registrar(pagamento.getId(), Resultado.RECUSADO, "PAY-1", null))
                .isInstanceOf(ConflitoDeRecursoException.class);
    }

    @Test
    void recusaPublicaPagamentoRecusadoComMotivoPadraoQuandoNaoInformado() {
        Pagamento comMotivo = pendente();
        Pagamento semMotivo = pendente();

        service.registrar(comMotivo.getId(), Resultado.RECUSADO, "PAY-2", "Cartao recusado pelo emissor");
        service.registrar(semMotivo.getId(), Resultado.RECUSADO, "PAY-3", " ");

        assertThat(publicador.tipos()).containsExactly(MensagensPagamento.PAGAMENTO_RECUSADO, MensagensPagamento.PAGAMENTO_RECUSADO);
        assertThat(publicador.payload(publicador.publicadas().get(0).envelope(), MensagensPagamento.PagamentoRecusado.class))
                .isEqualTo(new MensagensPagamento.PagamentoRecusado(comMotivo.getId(), "PAY-2", "Cartao recusado pelo emissor"));
        assertThat(semMotivo.getMotivo()).isEqualTo("Pagamento recusado");
    }

    @Test
    void resultadoAusenteOuPagamentoInexistenteSaoRecusados() {
        Pagamento pagamento = pendente();

        assertThatThrownBy(() -> service.registrar(pagamento.getId(), null, "PAY", null)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.registrar(UUID.randomUUID(), Resultado.APROVADO, "PAY", null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void expiraSoAsPendentesVencidasEPublicaPagamentoExpirado() {
        Pagamento vencida = pendente(UUID.randomUUID(), AGORA.minusMinutes(1), AGORA.minusDays(1));
        Pagamento noPrazo = pendente(UUID.randomUUID(), AGORA.plusMinutes(1), AGORA.minusDays(1));
        Pagamento paga = pendente(UUID.randomUUID(), AGORA.minusMinutes(1), AGORA.minusDays(1));
        paga.aprovar("PAY-4", AGORA.minusHours(1));

        assertThat(service.expirarVencidos(AGORA)).isEqualTo(1);

        assertThat(vencida.getStatus()).isEqualTo(StatusPagamento.EXPIRADO);
        assertThat(noPrazo.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(paga.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        Envelope evento = publicador.unica();
        assertThat(evento.type()).isEqualTo(MensagensPagamento.PAGAMENTO_EXPIRADO);
        assertThat(publicador.payload(evento, MensagensPagamento.PagamentoExpirado.class).pagamentoId()).isEqualTo(vencida.getId());
        assertThat(service.expirarVencidos(AGORA)).as("nada mais a expirar").isZero();
    }

    @Test
    void consultaPorOrcamentoDevolveACobrancaMaisRecente() {
        Orcamento orcamento = orcamentos.salvar(new Orcamento(
                "ORC-PAG", UUID.randomUUID(), ordem, UUID.randomUUID(), "Maria", "52998224725",
                "ABC1D23", "Toyota", "Corolla", "Troca de oleo", List.of("Troca de oleo"), List.of(),
                new BigDecimal("360"), BigDecimal.ZERO, AGORA, AGORA.plusDays(7), null, StatusOrcamento.APROVADO));
        pendente(orcamento.getId(), AGORA.plusDays(1), AGORA.minusHours(2)).recusar("PAY-5", "x", AGORA);
        Pagamento recente = pendente(orcamento.getId(), AGORA.plusDays(1), AGORA);

        assertThat(service.consultarPorOrcamento("ORC-PAG").getId()).isEqualTo(recente.getId());
        assertThat(service.consultar(recente.getId())).isSameAs(recente);
        assertThatThrownBy(() -> service.consultarPorOrcamento("ORC-NAO-EXISTE"))
                .isInstanceOf(RecursoNaoEncontradoException.class).hasMessageContaining("Orcamento nao encontrado");
        assertThatThrownBy(() -> service.consultar(UUID.randomUUID())).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void orcamentoAindaSemCobrancaResponde404ComOMotivo() {
        orcamentos.salvar(new Orcamento(
                "ORC-SEM", UUID.randomUUID(), ordem, UUID.randomUUID(), "Maria", "52998224725",
                "ABC1D23", "Toyota", "Corolla", "Troca de oleo", List.of("Troca de oleo"), List.of(),
                new BigDecimal("360"), BigDecimal.ZERO, AGORA, AGORA.plusDays(7), null, StatusOrcamento.AGUARDANDO_APROVACAO));

        assertThatThrownBy(() -> service.consultarPorOrcamento("ORC-SEM"))
                .isInstanceOf(RecursoNaoEncontradoException.class).hasMessageContaining("ainda sem cobranca");
    }

    @Test
    void cancelamentoDoOrcamentoCancelaSoACobrancaPendente() {
        UUID orcamentoId = UUID.randomUUID();
        Pagamento recusada = pendente(orcamentoId, AGORA.plusDays(1), AGORA.minusHours(1));
        recusada.recusar("PAY-6", "x", AGORA);
        Pagamento aberta = pendente(orcamentoId, AGORA.plusDays(1), AGORA);

        assertThat(cancelamento.cancelarPendente(orcamentoId)).isTrue();
        assertThat(aberta.getStatus()).isEqualTo(StatusPagamento.CANCELADO);
        assertThat(recusada.getStatus()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(cancelamento.cancelarPendente(orcamentoId)).as("nada mais pendente").isFalse();
        assertThat(cancelamento.cancelarPendente(UUID.randomUUID())).isFalse();
    }
}
