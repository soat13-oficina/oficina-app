package br.com.oficina.ordemservico.saga.infrastructure.observabilidade;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.oficina.ordemservico.saga.domain.model.PassoSaga;
import br.com.oficina.ordemservico.saga.domain.model.SagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.StatusSaga;
import br.com.oficina.ordemservico.saga.infrastructure.persistence.SpringDataSagaOrdemServicoRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Metricas da Saga da ordem de servico (PLANO-FASE-4.md, secao 12).
 *
 * <ul>
 *   <li>{@code oficina.saga.iniciadas} - Sagas criadas;</li>
 *   <li>{@code oficina.saga.finalizadas{resultado}} - Sagas que chegaram a um estado final;</li>
 *   <li>{@code oficina.saga.compensacoes{motivo}} - Sagas que entraram em compensacao;</li>
 *   <li>{@code oficina.saga.duracao{resultado}} - da abertura ao estado final;</li>
 *   <li>{@code oficina.saga.ativas{passo}} - o funil: quantas Sagas estao em cada passo agora;</li>
 *   <li>{@code oficina.saga.presas} - nao finalizadas com o prazo vencido. O agendador as trata em
 *       segundos; se o numero fica acima de zero, nada esta andando.</li>
 * </ul>
 *
 * <p>Os contadores saem da comparacao do estado que a Saga tinha ao ser carregada com o que esta sendo
 * gravado ({@link #aposSalvar}), entao uma transicao e contada uma vez so, onde quer que o orquestrador
 * a provoque. As tags saem de enums fechados; o {@code motivo} so vira tag se tiver o formato de um
 * codigo (o campo tambem guarda texto livre de falha, que explodiria a cardinalidade).
 */
@Component
public class MetricasSaga {
    private static final Duration VALIDADE_DO_CACHE = Duration.ofSeconds(15);
    private static final List<StatusSaga> FINAIS = Arrays.stream(StatusSaga.values()).filter(StatusSaga::isFinal).toList();

    private final MeterRegistry registry;
    private final SpringDataSagaOrdemServicoRepository sagas;

    private volatile Map<PassoSaga, Long> ativasPorPasso = new EnumMap<>(PassoSaga.class);
    private volatile long presas;
    private volatile LocalDateTime lidoEm = LocalDateTime.MIN;

    public MetricasSaga(MeterRegistry registry, SpringDataSagaOrdemServicoRepository sagas) {
        this.registry = registry;
        this.sagas = sagas;
        for (PassoSaga passo : PassoSaga.values()) {
            if (passo != PassoSaga.FIM) {
                Gauge.builder("oficina.saga.ativas", () -> ativas(passo))
                        .description("Sagas nao finalizadas por passo")
                        .tag("passo", passo.name())
                        .register(registry);
            }
        }
        Gauge.builder("oficina.saga.presas", this::presas)
                .description("Sagas nao finalizadas com o prazo vencido")
                .register(registry);
    }

    /**
     * @param antes estado da Saga quando foi carregada; {@code null} = Saga nova
     * @param saga  o que esta sendo gravado
     */
    public void aposSalvar(StatusSaga antes, SagaOrdemServico saga) {
        StatusSaga agora = saga.getStatus();
        if (antes == null) {
            registry.counter("oficina.saga.iniciadas").increment();
        }
        if (agora == StatusSaga.COMPENSANDO && antes != StatusSaga.COMPENSANDO) {
            registry.counter("oficina.saga.compensacoes", "motivo", motivo(saga.getMotivo())).increment();
        }
        if (agora.isFinal() && (antes == null || !antes.isFinal())) {
            String resultado = agora.name();
            registry.counter("oficina.saga.finalizadas", "resultado", resultado).increment();
            Timer.builder("oficina.saga.duracao")
                    .description("Da abertura da ordem ao estado final da Saga")
                    .tag("resultado", resultado)
                    .register(registry)
                    .record(Duration.between(saga.getCriadaEm(), saga.getAtualizadaEm()));
        }
    }

    static String motivo(String motivo) {
        return motivo != null && motivo.matches("[A-Z][A-Z0-9_]{1,59}") ? motivo : "OUTRO";
    }

    private double ativas(PassoSaga passo) {
        atualizarSeVelho();
        return ativasPorPasso.getOrDefault(passo, 0L);
    }

    private double presas() {
        atualizarSeVelho();
        return presas;
    }

    // Uma consulta por janela, e nao uma por gauge por scrape: o Datadog raspa a cada 15 s.
    private synchronized void atualizarSeVelho() {
        LocalDateTime agora = LocalDateTime.now();
        if (Duration.between(lidoEm, agora).compareTo(VALIDADE_DO_CACHE) < 0) {
            return;
        }
        Map<PassoSaga, Long> novo = new EnumMap<>(PassoSaga.class);
        for (Object[] linha : sagas.contarAtivasPorPasso(FINAIS)) {
            novo.put((PassoSaga) linha[0], (Long) linha[1]);
        }
        ativasPorPasso = novo;
        presas = sagas.buscarIdsComPrazoVencido(agora, FINAIS).size();
        lidoEm = agora;
    }

    /** Para os testes: esquece o cache e le de novo na proxima consulta. */
    void invalidarCache() {
        lidoEm = LocalDateTime.MIN;
    }
}
