package br.com.oficina.ordemservico.saga.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.oficina.common.application.mensageria.Filas;
import br.com.oficina.ordemservico.domain.model.MotivoEncerramento;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;

/** Maquina de estados da Saga (ADR 0004) no que o Passo 1 ja implementa. */
class SagaOrdemServicoTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 15, 13, 0);
    private static final Duration CINCO_MINUTOS = Duration.ofMinutes(5);
    private final UUID ordem = UUID.randomUUID();
    private final UUID orcamento = UUID.randomUUID();

    private SagaOrdemServico aguardandoDecisao() {
        SagaOrdemServico saga = SagaOrdemServico.iniciar(ordem, T0);
        saga.orcamentoGerado(orcamento, T0.plusDays(7), T0);
        return saga;
    }

    @Test
    void sagaNovaAguardaODiagnostico() {
        SagaOrdemServico saga = SagaOrdemServico.iniciar(ordem, T0);

        assertThat(saga.getId()).isNotNull();
        assertThat(saga.getOrdemDeServicoId()).isEqualTo(ordem);
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.DIAGNOSTICO);
        assertThat(saga.getCriadaEm()).isEqualTo(T0);
        assertThat(saga.getPrazoExpiraEm()).isNull();
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
    void caminhoFelizAteConcluida() {
        SagaOrdemServico saga = aguardandoDecisao();

        saga.orcamentoAprovado(orcamento, T0.plusHours(1));
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_EXECUCAO);
        assertThat(saga.getPrazoExpiraEm()).as("aprovado, a decisao nao vence mais").isNull();

        saga.execucaoIniciada(T0.plusHours(2));
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.EM_EXECUCAO);

        saga.execucaoConcluida(T0.plusHours(3));
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.CONCLUIDA);
        assertThat(saga.getStatus().isFinal()).isTrue();
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.FIM);
        assertThat(saga.getAtualizadaEm()).isEqualTo(T0.plusHours(3));
        assertThat(saga.descarregarComandos()).isEmpty();
    }

    @Test
    void rejeicaoEncerraSemExecucaoENaoCompensaNada() {
        SagaOrdemServico saga = aguardandoDecisao();

        saga.orcamentoRejeitado(orcamento, T0.plusHours(1));

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga.getMotivo()).isEqualTo("ORCAMENTO_RECUSADO");
        assertThat(saga.getPrazoExpiraEm()).isNull();
        assertThat(saga.prazoVencido(T0.plusYears(1))).isFalse();
        assertThat(saga.descarregarComandos()).isEmpty();
    }

    @Test
    void prazoDeDecisaoVencidoPedeOCancelamentoDoOrcamentoEEsperaAConfirmacao() {
        SagaOrdemServico saga = aguardandoDecisao();
        LocalDateTime vencimento = T0.plusDays(8);

        saga.prazoDecisaoVencido(vencimento, CINCO_MINUTOS);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.COMPENSANDO);
        assertThat(saga.getPassoAtual()).isEqualTo(PassoSaga.COMPENSACAO);
        assertThat(saga.getDesfecho()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga.getMotivo()).isEqualTo("ORCAMENTO_EXPIRADO");
        assertThat(saga.getCompensacoesPendentes()).containsExactly(MensagensSaga.ORCAMENTO_CANCELADO);
        assertThat(saga.getPrazoExpiraEm()).isEqualTo(vencimento.plus(CINCO_MINUTOS));
        assertThat(saga.descarregarComandos()).containsExactly(new ComandoSaga(
                MensagensSaga.CANCELAR_ORCAMENTO, Filas.BILLING_COMMANDS,
                new MensagensSaga.CancelarOrcamento(orcamento, "ORCAMENTO_EXPIRADO")));
        assertThat(saga.descarregarComandos()).as("descarregar esvazia").isEmpty();

        saga.compensacaoConfirmada(MensagensSaga.ORCAMENTO_CANCELADO, vencimento.plusMinutes(1));

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.ENCERRADA_SEM_EXECUCAO);
        assertThat(saga.getCompensacoesPendentes()).isEmpty();
        assertThat(saga.getPrazoExpiraEm()).isNull();
    }

    @Test
    void confirmacaoQueNaoEraEsperadaEhRecusada() {
        SagaOrdemServico saga = aguardandoDecisao();
        saga.prazoDecisaoVencido(T0.plusDays(8), CINCO_MINUTOS);

        assertThatThrownBy(() -> saga.compensacaoConfirmada("PagamentoEstornado", T0.plusDays(8)))
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
            assertThat(saga.descarregarComandos()).extracting(ComandoSaga::tipo).containsExactly(MensagensSaga.CANCELAR_ORCAMENTO);
        }

        assertThat(saga.respostaDaCompensacaoNaoChegou(agora.plusMinutes(6), CINCO_MINUTOS, 3)).isFalse();
        assertThat(saga.getStatus()).isEqualTo(StatusSaga.FALHA_COMPENSACAO);
        assertThat(saga.getStatus().isFinal()).isTrue();
        assertThat(saga.descarregarComandos()).isEmpty();
    }

    @Test
    void eventosForaDeOrdemSaoRecusadosSemMudarNada() {
        SagaOrdemServico saga = SagaOrdemServico.iniciar(ordem, T0);

        assertThatThrownBy(() -> saga.orcamentoAprovado(orcamento, T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.orcamentoRejeitado(orcamento, T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.prazoDecisaoVencido(T0, CINCO_MINUTOS)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.execucaoIniciada(T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.execucaoConcluida(T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.compensacaoConfirmada(MensagensSaga.ORCAMENTO_CANCELADO, T0))
                .isInstanceOf(TransicaoSagaInvalidaException.class);
        assertThatThrownBy(() -> saga.respostaDaCompensacaoNaoChegou(T0, CINCO_MINUTOS, 3))
                .isInstanceOf(TransicaoSagaInvalidaException.class);

        assertThat(saga.getStatus()).isEqualTo(StatusSaga.AGUARDANDO_DIAGNOSTICO);
        assertThat(saga.getOrcamentoId()).isNull();
    }

    @Test
    void orcamentoGeradoSoAntesDaDecisao() {
        SagaOrdemServico saga = aguardandoDecisao();

        assertThatThrownBy(() -> saga.orcamentoGerado(orcamento, T0, T0)).isInstanceOf(TransicaoSagaInvalidaException.class);
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
            "AGUARDANDO_EXECUCAO,,AGUARDANDO_EXECUCAO,EXECUCAO",
            "SERVICO_EM_ANDAMENTO,,EM_EXECUCAO,EXECUCAO",
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
