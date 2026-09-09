package br.com.oficina.ordemservico.infrastructure.observabilidade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.oficina.ordemservico.domain.model.OrdemDeServicoCriada;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MetricasOrdemServicoListenerTest {

    private static final UUID CLIENTE_ID = UUID.fromString("71111111-1111-1111-1111-111111111111");

    private SimpleMeterRegistry registry;
    private MetricasOrdemServicoListener listener;

    @BeforeEach
    void configurar() {
        registry = new SimpleMeterRegistry();
        listener = new MetricasOrdemServicoListener(registry);
    }

    @Test
    void deveContarOrdensCriadas() {
        listener.aoCriar(new OrdemDeServicoCriada("OS-001", CLIENTE_ID, UUID.randomUUID(), LocalDateTime.now()));
        listener.aoCriar(new OrdemDeServicoCriada("OS-002", CLIENTE_ID, UUID.randomUUID(), LocalDateTime.now()));

        assertEquals(2.0, registry.counter("os.criadas").count());
    }

    @Test
    void deveContarTransicaoComTagsDeOrigemEDestino() {
        listener.aoMudarSituacao(transicao(
                SituacaoOrdemDeServico.DIAGNOSTICO,
                SituacaoOrdemDeServico.AGUARDANDO_APROVACAO,
                LocalDateTime.of(2026, 6, 17, 8, 0),
                LocalDateTime.of(2026, 6, 17, 10, 0)));

        assertEquals(
                1.0,
                registry.counter(
                                "os.transicoes",
                                "situacao_anterior", "DIAGNOSTICO",
                                "situacao", "AGUARDANDO_APROVACAO")
                        .count());
    }

    @Test
    void deveMedirTempoNaSituacaoAnterior() {
        // Duas horas em Diagnóstico: a duração é atribuída à situação de ORIGEM, que é a que
        // acabou de terminar - taggear pelo destino faria "tempo médio em Execução" contar o
        // tempo que a ordem passou esperando aprovação.
        listener.aoMudarSituacao(transicao(
                SituacaoOrdemDeServico.DIAGNOSTICO,
                SituacaoOrdemDeServico.AGUARDANDO_APROVACAO,
                LocalDateTime.of(2026, 6, 17, 8, 0),
                LocalDateTime.of(2026, 6, 17, 10, 0)));

        Timer timer = registry.find("os.tempo_na_situacao").tag("situacao", "DIAGNOSTICO").timer();
        assertEquals(1L, timer.count());
        assertEquals(2.0, timer.totalTime(TimeUnit.HOURS), 0.0001);
    }

    @Test
    void naoDeveRegistrarTempoQuandoOrdemNaoTemMarcoAnterior() {
        // Ordem anterior à migration V19 e sem timestamp para o backfill: contar zero puxaria a
        // média da etapa para baixo e faria o dashboard mentir.
        listener.aoMudarSituacao(transicao(
                SituacaoOrdemDeServico.RECEBIDA,
                SituacaoOrdemDeServico.DIAGNOSTICO,
                null,
                LocalDateTime.of(2026, 6, 17, 10, 0)));

        assertNull(registry.find("os.tempo_na_situacao").timer());
        assertEquals(
                1.0,
                registry.counter(
                                "os.transicoes",
                                "situacao_anterior", "RECEBIDA",
                                "situacao", "DIAGNOSTICO")
                        .count());
    }

    @Test
    void deveAcumularTemposDaMesmaSituacaoEmOrdensDiferentes() {
        listener.aoMudarSituacao(transicao(
                SituacaoOrdemDeServico.EXECUCAO,
                SituacaoOrdemDeServico.FINALIZADA,
                LocalDateTime.of(2026, 6, 17, 8, 0),
                LocalDateTime.of(2026, 6, 17, 11, 0)));
        listener.aoMudarSituacao(transicao(
                SituacaoOrdemDeServico.EXECUCAO,
                SituacaoOrdemDeServico.FINALIZADA,
                LocalDateTime.of(2026, 6, 18, 8, 0),
                LocalDateTime.of(2026, 6, 18, 9, 0)));

        Timer timer = registry.find("os.tempo_na_situacao").tag("situacao", "EXECUCAO").timer();
        assertEquals(2L, timer.count());
        assertEquals(4.0, timer.totalTime(TimeUnit.HOURS), 0.0001);
        assertEquals(2.0, timer.mean(TimeUnit.HOURS), 0.0001);
    }

    private static StatusOrdemDeServicoAlterado transicao(
            SituacaoOrdemDeServico anterior,
            SituacaoOrdemDeServico nova,
            LocalDateTime anteriorDesde,
            LocalDateTime ocorridoEm) {
        return new StatusOrdemDeServicoAlterado("OS-001", CLIENTE_ID, anterior, nova, anteriorDesde, ocorridoEm);
    }
}
