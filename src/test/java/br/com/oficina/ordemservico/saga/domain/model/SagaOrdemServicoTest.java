package br.com.oficina.ordemservico.saga.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.ordemservico.domain.model.MotivoEncerramento;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;

/** Maquina de estados da Saga (ADR 0004). */
class SagaOrdemServicoTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 15, 13, 0);
    private static final Duration CINCO_MINUTOS = Duration.ofMinutes(5);
    private static final List<MensagensSaga.ItemPeca> ITENS = List.of(new MensagensSaga.ItemPeca("filtro-oleo", 2));
    private static final MensagensSaga.GerarOrcamento PEDIDO = new MensagensSaga.GerarOrcamento(
            "OS-1", null, null, null, "Troca de oleo", List.of(), ITENS, new BigDecimal("200.00"), null, null, null);
    private static final MensagensSaga.EnfileirarDiagnostico FILA = new MensagensSaga.EnfileirarDiagnostico(
            "OS-1", new MensagensSaga.Veiculo("ABC1D23", "Toyota", "Corolla", 2020), List.of(), List.of(),
            Instant.parse("2026-10-15T13:00:00Z"));
    private static final Instant EXPIRA_PAGAMENTO = Instant.parse("2026-10-16T16:00:00Z");
    private static final String LINK = "https://pagamento-simulado.oficina.local/checkout/1";

    private final UUID ordem = UUID.randomUUID();
    private final UUID orcamento = UUID.randomUUID();
    private final UUID pagamento = UUID.randomUUID();

    /** Aberta, com o EnfileirarDiagnostico ainda por enviar. */
    private SagaOrdemServico aberta() {
        return SagaOrdemServico.iniciar(ordem, FILA, T0, CINCO_MINUTOS);
    }

    /** Fila de diagnostico confirmada: a Saga espera o mecanico, sem prazo. */
    private SagaOrdemServico aguardandoMecanico() {
        SagaOrdemServico saga = aberta();
        saga.descarregarComandos();
        saga.diagnosticoEnfileirado(T0);
        return saga;
    }

    private SagaOrdemServico gerandoOrcamento() {
        SagaOrdemServico saga = aguardandoMecanico();
        saga.diagnosticoConcluido(PEDIDO, T0, CINCO_MINUTOS);
        return saga;
    }

    private SagaOrdemServico aguardandoDecisao() {
        SagaOrdemServico saga = gerandoOrcamento();
        saga.orcamentoGerado(orcamento, T0.plusDays(7), T0);
        saga.descarregarComandos();
        return saga;
    }

    private SagaOrdemServico reservandoPecas() {
        SagaOrdemServico saga = aguardandoDecisao();
        saga.orcamentoAprovado(orcamento, ITENS, T0.plusHours(1), CINCO_MINUTOS);
        saga.descarregarComandos();
        return saga;
    }

    private SagaOrdemServico aguardandoCobranca() {
        SagaOrdemServico saga = reservandoPecas();
        saga.pecasReservadas(EXPIRA_PAGAMENTO, T0.plusHours(1), CINCO_MINUTOS);
        saga.descarregarComandos();
        return saga;
    }

    private SagaOrdemServico aguardandoPagamento() {
        SagaOrdemServico saga = aguardandoCobranca();
        saga.pagamentoSolicitado(pagamento, LINK, T0.plusDays(1), T0.plusHours(1));
        return saga;
    }

    private SagaOrdemServico emExecucao() {
        SagaOrdemServico saga = aguardandoPagamento();
        saga.pagamentoConfirmado(pagamento, T0.plusHours(2), CINCO_MINUTOS);
        saga.execucaoEnfileirada(T0.plusHours(2));
        saga.descarregarComandos();
        saga.execucaoIniciada(T0.plusHours(3));
        return saga;
    }

    /** Pago, com o EnfileirarExecucao ainda sem resposta. */
    private SagaOrdemServico enfileirandoExecucao() {
        SagaOrdemServico saga = aguardandoPagamento();
        saga.pagamentoConfirmado(pagamento, T0.plusHours(2), CINCO_MINUTOS);
        saga.descarregarComandos();
        return saga;
    }

    @Test
    void sagaNovaMandaAOsParaAFilaDeDiagnosticoEVigiaAResposta() {
        SagaOrdemServico saga = aberta();

        assertThat(saga.getId()).isNotNull();
        assertThat(saga.getOrdemDeServicoId()).isEqualTo(ordem);
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.DIAGNOSTICO);
        assertThat(saga.getCriadaEm()).isEqualTo(T0);
        assertThat(saga.getPrazoExpiraEm()).as("prazo de resposta da fila").isEqualTo(T0.plus(CINCO_MINUTOS));
        assertThat(saga.aguardandoConfirmacaoDaFila()).isTrue();
        assertThat(saga.descarregarComandos()).containsExactly(
                new ComandoSaga(MensagensSaga.ENFILEIRAR_DIAGNOSTICO, Filas.EXECUTION_COMMANDS, FILA));
    }

    @Test
    void filaConfirmadaDeixaAOsEsperandoOMecanicoSemPrazo() {
        SagaOrdemServico saga = aguardandoMecanico();

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga.getPrazoExpiraEm()).as("o diagnostico depende do mecanico").isNull();
        assertThat(saga.aguardandoConfirmacaoDaFila()).isFalse();
        assertThat(saga.prazoVencido(T0.plusYears(1))).isFalse();
        assertThat(saga.descarregarComandos()).isEmpty();
    }

    @Test
    void filaDeDiagnosticoSemRespostaEhReenviadaAteOLimiteEDepoisExigeAcaoManual() {
        SagaOrdemServico saga = aberta();
        saga.descarregarComandos();
        LocalDateTime agora = T0;

        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            agora = agora.plusMinutes(6);
            assertThat(saga.reenviarComandoDeAvanco(agora, CINCO_MINUTOS, 3)).isTrue();
            assertThat(saga.descarregarComandos())
                    .containsExactly(ComandoSaga.reenvio(MensagensSaga.ENFILEIRAR_DIAGNOSTICO, Filas.EXECUTION_COMMANDS));
        }
        assertThat(saga.reenviarComandoDeAvanco(agora.plusMinutes(6), CINCO_MINUTOS, 3)).isFalse();

        saga.filaSemConfirmacao("Execution nao confirmou a fila", agora.plusMinutes(6));

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.FALHA_COMPENSACAO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.FIM);
        assertThat(saga.getMotivo()).isEqualTo("Execution nao confirmou a fila");
        assertThat(saga.getPrazoExpiraEm()).isNull();
    }

    @Test
    void diagnosticoConcluidoMandaGerarOrcamentoEEsperaAResposta() {
        SagaOrdemServico saga = gerandoOrcamento();

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.GERANDO_ORCAMENTO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.ORCAMENTO);
        assertThat(saga.getPrazoExpiraEm()).isEqualTo(T0.plus(CINCO_MINUTOS));
        assertThat(saga.descarregarComandos()).containsExactly(
                new ComandoSaga(MensagensSaga.GERAR_ORCAMENTO, Filas.BILLING_COMMANDS, PEDIDO));
    }

    @Test
    void orcamentoGeradoAbreOPrazoDeDecisao() {
        SagaOrdemServico saga = aguardandoDecisao();

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.DECISAO);
        assertThat(saga.getOrcamentoId()).isEqualTo(orcamento);
        assertThat(saga.getPrazoExpiraEm()).isEqualTo(T0.plusDays(7));
        assertThat(saga.prazoVencido(T0.plusDays(6))).isFalse();
        assertThat(saga.prazoVencido(T0.plusDays(8))).isTrue();
    }

    @Test
    void orcamentoQueFalhaReabreODiagnosticoEVoltaAEsperarOMecanicoQuandoAExecutionConfirma() {
        SagaOrdemServico saga = gerandoOrcamento();
        saga.descarregarComandos();

        saga.orcamentoFalhou("Peca inexistente: X", List.of("X"), T0.plusMinutes(1), CINCO_MINUTOS);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(saga.getDesfecho()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga.getMotivo()).isEqualTo("Peca inexistente: X");
        assertThat(saga.getCompensacoesPendentes()).containsExactly(MensagensSaga.DIAGNOSTICO_REABERTO);
        assertThat(saga.getPrazoExpiraEm()).isEqualTo(T0.plusMinutes(1).plus(CINCO_MINUTOS));
        assertThat(saga.descarregarComandos()).containsExactly(new ComandoSaga(
                MensagensSaga.REABRIR_DIAGNOSTICO, Filas.EXECUTION_COMMANDS,
                new MensagensSaga.ReabrirDiagnostico("Peca inexistente: X", List.of("X"))));

        saga.compensacaoConfirmada(MensagensSaga.DIAGNOSTICO_REABERTO, T0.plusMinutes(2), null);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.DIAGNOSTICO);
        assertThat(saga.getDesfecho()).isNull();
        assertThat(saga.getPrazoExpiraEm()).as("o mecanico nao tem prazo").isNull();
        assertThat(saga.getMotivo()).isEqualTo("Peca inexistente: X");

        saga.diagnosticoConcluido(PEDIDO, T0.plusHours(1), CINCO_MINUTOS);
        assertThat(saga.getStatus()).as("novo diagnostico, novo orcamento").isEqualTo(StatusSaga.GERANDO_ORCAMENTO);
        assertThat(saga.getMotivo()).as("novo orcamento limpa o motivo da falha").isNull();
    }

    @Test
    void reaberturaSemConfirmacaoEPedidaDeNovoComOMesmoPayload() {
        SagaOrdemServico saga = gerandoOrcamento();
        saga.orcamentoFalhou("Billing nao respondeu", null, T0.plusMinutes(1), CINCO_MINUTOS);
        saga.descarregarComandos();

        assertThat(saga.respostaDaCompensacaoNaoChegou(T0.plusMinutes(8), CINCO_MINUTOS, 3)).isTrue();

        assertThat(saga.descarregarComandos())
                .containsExactly(ComandoSaga.reenvio(MensagensSaga.REABRIR_DIAGNOSTICO, Filas.EXECUTION_COMMANDS));
    }

    @Test
    void motivoLongoEhTruncado() {
        SagaOrdemServico saga = gerandoOrcamento();

        saga.orcamentoFalhou("x".repeat(300), null, T0, CINCO_MINUTOS);

        assertThat(saga.getMotivo()).hasSize(255).endsWith("...");
    }

    @Test
    void caminhoFelizAteConcluida() {
        SagaOrdemServico saga = aguardandoDecisao();

        saga.orcamentoAprovado(orcamento, ITENS, T0.plusHours(1), CINCO_MINUTOS);
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.RESERVANDO_PECAS);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.RESERVA);
        assertThat(saga.getPrazoExpiraEm()).as("prazo de resposta da Execution").isEqualTo(T0.plusHours(1).plus(CINCO_MINUTOS));
        assertThat(saga.descarregarComandos()).containsExactly(new ComandoSaga(
                MensagensSaga.RESERVAR_PECAS, Filas.EXECUTION_COMMANDS, new MensagensSaga.ReservarPecas(orcamento, ITENS)));

        saga.pecasReservadas(EXPIRA_PAGAMENTO, T0.plusHours(1), CINCO_MINUTOS);
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_PAGAMENTO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.PAGAMENTO);
        assertThat(saga.aguardandoCobranca()).isTrue();
        assertThat(saga.descarregarComandos()).containsExactly(new ComandoSaga(
                MensagensSaga.SOLICITAR_PAGAMENTO, Filas.BILLING_COMMANDS,
                new MensagensSaga.SolicitarPagamento(orcamento, EXPIRA_PAGAMENTO)));

        saga.pagamentoSolicitado(pagamento, LINK, T0.plusDays(1), T0.plusHours(1));
        assertThat(saga.aguardandoCobranca()).isFalse();
        assertThat(saga.getPagamentoId()).isEqualTo(pagamento);
        assertThat(saga.getLinkPagamento()).isEqualTo(LINK);
        assertThat(saga.getPrazoExpiraEm()).as("prazo do pagamento + margem").isEqualTo(T0.plusDays(1));

        saga.pagamentoConfirmado(pagamento, T0.plusHours(2), CINCO_MINUTOS);
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_EXECUCAO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.EXECUCAO);
        assertThat(saga.getPrazoExpiraEm()).as("vigia a resposta da fila de execucao").isEqualTo(T0.plusHours(2).plus(CINCO_MINUTOS));
        assertThat(saga.aguardandoConfirmacaoDaFila()).isTrue();
        assertThat(saga.descarregarComandos()).containsExactly(new ComandoSaga(
                MensagensSaga.ENFILEIRAR_EXECUCAO, Filas.EXECUTION_COMMANDS, new MensagensSaga.EnfileirarExecucao(orcamento)));

        saga.execucaoEnfileirada(T0.plusHours(2));
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_EXECUCAO);
        assertThat(saga.getPrazoExpiraEm()).as("na fila, a execucao e do mecanico").isNull();

        saga.execucaoIniciada(T0.plusHours(3));
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.EM_EXECUCAO);

        saga.execucaoConcluida(T0.plusHours(4));
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.CONCLUIDA);
        assertThat(saga.getStatus().isFinal()).isTrue();
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.FIM);
        assertThat(saga.getAtualizadaEm()).isEqualTo(T0.plusHours(4));
        assertThat(saga.descarregarComandos()).isEmpty();
    }

    @Test
    void rejeicaoEncerraSemExecucaoDepoisDeCancelarAExecucao() {
        SagaOrdemServico saga = aguardandoDecisao();

        saga.orcamentoRejeitado(orcamento, T0.plusHours(1), CINCO_MINUTOS);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(saga.getDesfecho()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga.getMotivo()).isEqualTo("ORCAMENTO_RECUSADO");
        assertThat(saga.getCompensacoesPendentes()).containsExactly(MensagensSaga.EXECUCAO_CANCELADA);
        assertThat(saga.descarregarComandos()).containsExactly(new ComandoSaga(
                MensagensSaga.CANCELAR_EXECUCAO, Filas.EXECUTION_COMMANDS, new MensagensSaga.CancelarExecucao("ORCAMENTO_RECUSADO")));

        saga.compensacaoConfirmada(MensagensSaga.EXECUCAO_CANCELADA, T0.plusHours(2), null);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga.getPrazoExpiraEm()).isNull();
        assertThat(saga.prazoVencido(T0.plusYears(1))).isFalse();
        assertThat(saga.descarregarComandos()).isEmpty();
    }

    @Test
    void prazoDeDecisaoVencidoPedeOCancelamentoDoOrcamentoEDaExecucaoEEsperaAsConfirmacoes() {
        SagaOrdemServico saga = aguardandoDecisao();
        LocalDateTime vencimento = T0.plusDays(8);

        saga.prazoDecisaoVencido(vencimento, CINCO_MINUTOS);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.COMPENSACAO);
        assertThat(saga.getDesfecho()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga.getMotivo()).isEqualTo("ORCAMENTO_EXPIRADO");
        assertThat(saga.getCompensacoesPendentes())
                .containsExactlyInAnyOrder(MensagensSaga.ORCAMENTO_CANCELADO, MensagensSaga.EXECUCAO_CANCELADA);
        assertThat(saga.getPrazoExpiraEm()).isEqualTo(vencimento.plus(CINCO_MINUTOS));
        assertThat(saga.descarregarComandos()).containsExactly(
                new ComandoSaga(MensagensSaga.CANCELAR_EXECUCAO, Filas.EXECUTION_COMMANDS,
                        new MensagensSaga.CancelarExecucao("ORCAMENTO_EXPIRADO")),
                new ComandoSaga(MensagensSaga.CANCELAR_ORCAMENTO, Filas.BILLING_COMMANDS,
                        new MensagensSaga.CancelarOrcamento(orcamento, "ORCAMENTO_EXPIRADO")));
        assertThat(saga.descarregarComandos()).as("descarregar esvazia").isEmpty();

        saga.compensacaoConfirmada(MensagensSaga.ORCAMENTO_CANCELADO, vencimento.plusMinutes(1), null);
        assertThat(saga.getStatus()).as("falta a execucao").isEqualTo(StatusSaga.COMPENSANDO);
        saga.compensacaoConfirmada(MensagensSaga.EXECUCAO_CANCELADA, vencimento.plusMinutes(2), null);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.FIM);
        assertThat(saga.getCompensacoesPendentes()).isEmpty();
        assertThat(saga.getPrazoExpiraEm()).isNull();
    }

    @Test
    void estoqueInsuficienteReverteAAprovacaoEVoltaAEsperarADecisaoComPrazoNovo() {
        SagaOrdemServico saga = reservandoPecas();
        List<MensagensSaga.PecaFaltante> faltantes = List.of(new MensagensSaga.PecaFaltante("filtro-oleo", 2, 1));
        LocalDateTime agora = T0.plusHours(2);

        saga.reservaFalhou("Estoque insuficiente", faltantes, agora, CINCO_MINUTOS);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.REVERTENDO_APROVACAO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.RESERVA);
        assertThat(saga.getDesfecho()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(saga.getCompensacoesPendentes()).containsExactly(MensagensSaga.APROVACAO_REVERTIDA);
        assertThat(saga.descarregarComandos()).containsExactly(new ComandoSaga(
                MensagensSaga.REVERTER_APROVACAO, Filas.BILLING_COMMANDS,
                new MensagensSaga.ReverterAprovacao(orcamento, "Estoque insuficiente", faltantes)));

        saga.compensacaoConfirmada(MensagensSaga.APROVACAO_REVERTIDA, agora.plusMinutes(1), agora.plusDays(7));

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.DECISAO);
        assertThat(saga.getDesfecho()).isNull();
        assertThat(saga.getPrazoExpiraEm()).as("prazo de decisao novo").isEqualTo(agora.plusDays(7));
        assertThat(saga.getMotivo()).isEqualTo("Estoque insuficiente");

        saga.orcamentoAprovado(orcamento, ITENS, agora.plusDays(1), CINCO_MINUTOS);
        assertThat(saga.getStatus()).as("o cliente pode aprovar de novo").isEqualTo(StatusSaga.RESERVANDO_PECAS);
        assertThat(saga.getMotivo()).isNull();
    }

    @Test
    void reservaSemRespostaLiberaOQueHouverEReverteAAprovacao() {
        SagaOrdemServico saga = reservandoPecas();
        LocalDateTime agora = T0.plusHours(2);

        saga.reservaSemResposta(agora, CINCO_MINUTOS);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.REVERTENDO_APROVACAO);
        assertThat(saga.getCompensacoesPendentes())
                .containsExactlyInAnyOrder(MensagensSaga.APROVACAO_REVERTIDA, MensagensSaga.RESERVA_LIBERADA);
        String motivo = saga.getMotivo();
        assertThat(saga.descarregarComandos()).containsExactly(
                new ComandoSaga(MensagensSaga.REVERTER_APROVACAO, Filas.BILLING_COMMANDS,
                        new MensagensSaga.ReverterAprovacao(orcamento, motivo, null)),
                new ComandoSaga(MensagensSaga.LIBERAR_RESERVA, Filas.EXECUTION_COMMANDS,
                        new MensagensSaga.LiberarReserva(motivo)));

        saga.compensacaoConfirmada(MensagensSaga.RESERVA_LIBERADA, agora.plusMinutes(1), agora.plusDays(7));
        assertThat(saga.getStatus()).as("falta uma confirmacao").isEqualTo(StatusSaga.REVERTENDO_APROVACAO);
        assertThat(saga.getCompensacoesPendentes()).containsExactly(MensagensSaga.APROVACAO_REVERTIDA);

        saga.compensacaoConfirmada(MensagensSaga.APROVACAO_REVERTIDA, agora.plusMinutes(2), agora.plusDays(7));
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
    }

    @ParameterizedTest
    @EnumSource(value = MotivoEncerramento.class, names = {"PAGAMENTO_RECUSADO", "PAGAMENTO_EXPIRADO"})
    void pagamentoNaoConcluidoLiberaAsPecasECancelaOOrcamentoEAExecucao(MotivoEncerramento motivo) {
        SagaOrdemServico saga = aguardandoPagamento();
        LocalDateTime agora = T0.plusHours(5);

        saga.pagamentoNaoConcluido(motivo, agora, CINCO_MINUTOS);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.COMPENSACAO);
        assertThat(saga.getDesfecho()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(saga.getMotivo()).isEqualTo(motivo.name());
        assertThat(saga.descarregarComandos()).containsExactly(
                new ComandoSaga(MensagensSaga.CANCELAR_EXECUCAO, Filas.EXECUTION_COMMANDS,
                        new MensagensSaga.CancelarExecucao(motivo.name())),
                new ComandoSaga(MensagensSaga.CANCELAR_ORCAMENTO, Filas.BILLING_COMMANDS,
                        new MensagensSaga.CancelarOrcamento(orcamento, motivo.name())),
                new ComandoSaga(MensagensSaga.LIBERAR_RESERVA, Filas.EXECUTION_COMMANDS,
                        new MensagensSaga.LiberarReserva(motivo.name())));

        saga.compensacaoConfirmada(MensagensSaga.ORCAMENTO_CANCELADO, agora.plusMinutes(1), null);
        saga.compensacaoConfirmada(MensagensSaga.RESERVA_LIBERADA, agora.plusMinutes(2), null);
        assertThat(saga.getStatus()).as("falta a execucao").isEqualTo(StatusSaga.COMPENSANDO);
        saga.compensacaoConfirmada(MensagensSaga.EXECUCAO_CANCELADA, agora.plusMinutes(3), null);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.FIM);
        assertThat(saga.getPrazoExpiraEm()).isNull();
    }

    @Test
    void execucaoAbortadaEstornaOPagamentoELiberaAsPecas() {
        SagaOrdemServico saga = emExecucao();
        LocalDateTime agora = T0.plusHours(5);

        saga.execucaoFalhou(agora, CINCO_MINUTOS);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(saga.getMotivo()).isEqualTo("FALHA_EXECUCAO");
        assertThat(saga.getDesfecho()).isEqualTo(StatusSaga.COMPENSADA);
        assertThat(saga.descarregarComandos()).containsExactly(
                new ComandoSaga(MensagensSaga.ESTORNAR_PAGAMENTO, Filas.BILLING_COMMANDS,
                        new MensagensSaga.EstornarPagamento(orcamento, pagamento, "FALHA_EXECUCAO")),
                new ComandoSaga(MensagensSaga.LIBERAR_RESERVA, Filas.EXECUTION_COMMANDS,
                        new MensagensSaga.LiberarReserva("FALHA_EXECUCAO")));
    }

    @Test
    void execucaoAbortadaDeSagaAdotadaSemPagamentoSoLiberaAsPecas() {
        SagaOrdemServico saga = SagaOrdemServico.adotar(ordem, StatusOrdemDeServico.SERVICO_EM_ANDAMENTO, null, T0);

        saga.execucaoFalhou(T0.plusHours(1), CINCO_MINUTOS);

        assertThat(saga.getCompensacoesPendentes()).containsExactly(MensagensSaga.RESERVA_LIBERADA);
        assertThat(saga.descarregarComandos()).extracting(ComandoSaga::tipo).containsExactly(MensagensSaga.LIBERAR_RESERVA);
    }

    @Test
    void estornoRecusadoContinuaPendenteEEhPedidoDeNovoQuandoOPrazoVence() {
        SagaOrdemServico saga = emExecucao();
        LocalDateTime agora = T0.plusHours(5);
        saga.execucaoFalhou(agora, CINCO_MINUTOS);
        saga.descarregarComandos();
        saga.compensacaoConfirmada(MensagensSaga.RESERVA_LIBERADA, agora.plusMinutes(1), null);

        saga.compensacaoFalhou(MensagensSaga.PAGAMENTO_ESTORNADO, agora.plusMinutes(2), CINCO_MINUTOS);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(saga.getCompensacoesPendentes()).containsExactly(MensagensSaga.PAGAMENTO_ESTORNADO);
        assertThat(saga.getPrazoExpiraEm()).isEqualTo(agora.plusMinutes(2).plus(CINCO_MINUTOS));
        assertThat(saga.descarregarComandos()).as("o reenvio espera o prazo").isEmpty();

        assertThat(saga.respostaDaCompensacaoNaoChegou(agora.plusMinutes(8), CINCO_MINUTOS, 3)).isTrue();
        assertThat(saga.descarregarComandos()).as("so o que continua pendente")
                .extracting(ComandoSaga::tipo).containsExactly(MensagensSaga.ESTORNAR_PAGAMENTO);
        assertThatThrownBy(() -> saga.compensacaoFalhou(MensagensSaga.RESERVA_LIBERADA, agora, CINCO_MINUTOS))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
    }

    @Test
    void confirmacaoQueNaoEraEsperadaEhRecusada() {
        SagaOrdemServico saga = aguardandoDecisao();
        saga.prazoDecisaoVencido(T0.plusDays(8), CINCO_MINUTOS);

        assertThatThrownBy(() -> saga.compensacaoConfirmada(MensagensSaga.PAGAMENTO_ESTORNADO, T0.plusDays(8), null))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
    }

    @Test
    void confirmacaoQueNaoChegaEhPedidaDeNovoAteOLimiteEDepoisViraFalhaDeCompensacao() {
        SagaOrdemServico saga = aguardandoDecisao();
        LocalDateTime agora = T0.plusDays(8);
        saga.prazoDecisaoVencido(agora, CINCO_MINUTOS);
        saga.descarregarComandos();

        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            agora = agora.plusMinutes(6);
            assertThat(saga.respostaDaCompensacaoNaoChegou(agora, CINCO_MINUTOS, 3)).isTrue();
            assertThat(saga.getTentativasPasso()).isEqualTo(tentativa);
            assertThat(saga.descarregarComandos()).extracting(ComandoSaga::tipo)
                    .containsExactly(MensagensSaga.CANCELAR_EXECUCAO, MensagensSaga.CANCELAR_ORCAMENTO);
        }

        assertThat(saga.respostaDaCompensacaoNaoChegou(agora.plusMinutes(6), CINCO_MINUTOS, 3)).isFalse();
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.FALHA_COMPENSACAO);
        assertThat(saga.getStatus().isFinal()).isTrue();
        assertThat(saga.descarregarComandos()).isEmpty();
    }

    @Test
    void comandoDeAvancoSemRespostaEhReenviadoAteOLimite() {
        SagaOrdemServico saga = gerandoOrcamento();
        saga.descarregarComandos();
        LocalDateTime agora = T0;

        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            agora = agora.plusMinutes(6);
            assertThat(saga.reenviarComandoDeAvanco(agora, CINCO_MINUTOS, 3)).isTrue();
            assertThat(saga.getTentativasPasso()).isEqualTo(tentativa);
            assertThat(saga.getPrazoExpiraEm()).isEqualTo(agora.plus(CINCO_MINUTOS));
            assertThat(saga.descarregarComandos())
                    .containsExactly(ComandoSaga.reenvio(MensagensSaga.GERAR_ORCAMENTO, Filas.BILLING_COMMANDS));
        }

        assertThat(saga.reenviarComandoDeAvanco(agora.plusMinutes(6), CINCO_MINUTOS, 3)).isFalse();
        assertThat(saga.getStatus()).as("quem trata a desistencia e o orquestrador").isEqualTo(StatusSaga.GERANDO_ORCAMENTO);
        assertThat(saga.descarregarComandos()).isEmpty();
    }

    @Test
    void reenvioDaReservaEDaCobrancaVaiParaOParticipanteCerto() {
        SagaOrdemServico reservando = reservandoPecas();
        assertThat(reservando.reenviarComandoDeAvanco(T0.plusHours(2), CINCO_MINUTOS, 3)).isTrue();
        assertThat(reservando.descarregarComandos())
                .containsExactly(ComandoSaga.reenvio(MensagensSaga.RESERVAR_PECAS, Filas.EXECUTION_COMMANDS));

        SagaOrdemServico semCobranca = aguardandoCobranca();
        assertThat(semCobranca.reenviarComandoDeAvanco(T0.plusHours(2), CINCO_MINUTOS, 3)).isTrue();
        assertThat(semCobranca.descarregarComandos())
                .containsExactly(ComandoSaga.reenvio(MensagensSaga.SOLICITAR_PAGAMENTO, Filas.BILLING_COMMANDS));

        semCobranca.pagamentoSolicitado(pagamento, LINK, T0.plusDays(1), T0.plusHours(2));
        assertThat(semCobranca.getTentativasPasso()).as("cobranca criada zera as tentativas").isZero();
    }

    @Test
    void filaDeExecucaoSemRespostaEhReenviadaEDepoisExigeAcaoManual() {
        SagaOrdemServico saga = enfileirandoExecucao();
        LocalDateTime agora = T0.plusHours(3);

        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            agora = agora.plusMinutes(6);
            assertThat(saga.reenviarComandoDeAvanco(agora, CINCO_MINUTOS, 3)).isTrue();
            assertThat(saga.descarregarComandos())
                    .containsExactly(ComandoSaga.reenvio(MensagensSaga.ENFILEIRAR_EXECUCAO, Filas.EXECUTION_COMMANDS));
        }
        assertThat(saga.reenviarComandoDeAvanco(agora.plusMinutes(6), CINCO_MINUTOS, 3)).isFalse();

        saga.filaSemConfirmacao("Execution nao confirmou a fila", agora.plusMinutes(6));

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.FALHA_COMPENSACAO);
    }

    @Test
    void semComandoAguardandoRespostaNaoHaReenvio() {
        SagaOrdemServico decisao = aguardandoDecisao();
        SagaOrdemServico comCobranca = aguardandoPagamento();

        assertThatThrownBy(() -> decisao.reenviarComandoDeAvanco(T0, CINCO_MINUTOS, 3))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> comCobranca.reenviarComandoDeAvanco(T0, CINCO_MINUTOS, 3))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
    }

    @Test
    void eventosForaDeOrdemSaoRecusadosSemMudarNada() {
        SagaOrdemServico saga = aberta();
        saga.descarregarComandos();

        assertThatThrownBy(() -> saga.orcamentoGerado(orcamento, T0, T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.orcamentoFalhou("x", null, T0, CINCO_MINUTOS)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.orcamentoAprovado(orcamento, ITENS, T0, CINCO_MINUTOS))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.orcamentoRejeitado(orcamento, T0, CINCO_MINUTOS)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.prazoDecisaoVencido(T0, CINCO_MINUTOS)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.pecasReservadas(EXPIRA_PAGAMENTO, T0, CINCO_MINUTOS))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.reservaFalhou("x", List.of(), T0, CINCO_MINUTOS))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.reservaSemResposta(T0, CINCO_MINUTOS)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.pagamentoSolicitado(pagamento, LINK, T0, T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.pagamentoConfirmado(pagamento, T0, CINCO_MINUTOS)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.pagamentoNaoConcluido(MotivoEncerramento.PAGAMENTO_RECUSADO, T0, CINCO_MINUTOS))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.execucaoEnfileirada(T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.execucaoIniciada(T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.execucaoConcluida(T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.execucaoFalhou(T0, CINCO_MINUTOS)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.compensacaoConfirmada(MensagensSaga.ORCAMENTO_CANCELADO, T0, null))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.compensacaoFalhou(MensagensSaga.PAGAMENTO_ESTORNADO, T0, CINCO_MINUTOS))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.respostaDaCompensacaoNaoChegou(T0, CINCO_MINUTOS, 3))
                .isInstanceOf(TransicaoSagaInvalidaException.class);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga.getOrcamentoId()).isNull();
        assertThat(saga.descarregarComandos()).isEmpty();
    }

    @Test
    void diagnosticoSoPodeGerarOrcamentoUmaVezEnquantoAguardaAResposta() {
        SagaOrdemServico saga = gerandoOrcamento();

        assertThatThrownBy(() -> saga.diagnosticoConcluido(PEDIDO, T0, CINCO_MINUTOS)).isInstanceOf(TransicaoSagaInvalidaException.class);
    }

    @Test
    void prazoDescartadoNaoVenceMais() {
        SagaOrdemServico saga = aguardandoDecisao();

        saga.descartarPrazo(T0.plusDays(8));

        assertThat(saga.getPrazoExpiraEm()).isNull();
        assertThat(saga.prazoVencido(T0.plusYears(1))).isFalse();
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DECISAO);
    }

    @ParameterizedTest
    @CsvSource({
            "OS_ABERTA,,AGUARDANDO_DIAGNOSTICO,DIAGNOSTICO",
            "DIAGNOSTICO_EM_ANDAMENTO,,AGUARDANDO_DIAGNOSTICO,DIAGNOSTICO",
            "DIAGNOSTICO_CONCLUIDO,,AGUARDANDO_DIAGNOSTICO,DIAGNOSTICO",
            "ORCAMENTO_GERADO,,AGUARDANDO_DECISAO,DECISAO",
            "AGUARDANDO_APROVACAO,,AGUARDANDO_DECISAO,DECISAO",
            "AGUARDANDO_PAGAMENTO,,AGUARDANDO_PAGAMENTO,PAGAMENTO",
            "AGUARDANDO_EXECUCAO,,AGUARDANDO_EXECUCAO,EXECUCAO",
            "SERVICO_EM_ANDAMENTO,,EM_EXECUCAO,EXECUCAO",
            "CANCELADA,PAGAMENTO_RECUSADO,COMPENSADA,FIM",
            "OS_FINALIZADA,SERVICO_CONCLUIDO,CONCLUIDA,FIM",
            "OS_FINALIZADA,,CONCLUIDA,FIM",
            "OS_FINALIZADA,ORCAMENTO_RECUSADO,ENCERRADA_SEM_EXECUCAO,FIM",
            "OS_FINALIZADA,ORCAMENTO_EXPIRADO,ENCERRADA_SEM_EXECUCAO,FIM",
            "ENTREGUE,SERVICO_CONCLUIDO,CONCLUIDA,FIM"
    })
    void adocaoDerivaOEstadoDoStatusDaOrdem(StatusOrdemDeServico statusOrdem, MotivoEncerramento motivo,
                                           StatusSaga esperado, PassoSaga passo) {
        SagaOrdemServico saga = SagaOrdemServico.adotar(ordem, statusOrdem, motivo, T0);

        assertThat(saga.getStatus()).isEqualTo(esperado);
        assertThat(saga.getPassoAtual()).isEqualTo(passo);
        assertThat(saga.getPrazoExpiraEm()).as("saga adotada nao tem prazo").isNull();
    }

    @Test
    void registroDePassoTruncaDetalheLongo() {
        RegistroPassoSaga registro = new RegistroPassoSaga(UUID.randomUUID(), PassoSaga.DECISAO,
                RegistroPassoSaga.Tipo.EVENTO_IGNORADO, null, "x".repeat(3000), T0);

        assertThat(registro.getDetalhe()).hasSize(2000);
        assertThat(new RegistroPassoSaga(UUID.randomUUID(), PassoSaga.FIM, RegistroPassoSaga.Tipo.TIMEOUT, null, null, T0)
                .getDetalhe()).isNull();
    }
}
