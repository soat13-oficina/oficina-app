package br.com.oficina.ordemservico.infrastructure.observabilidade;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.com.oficina.ordemservico.domain.model.OrdemDeServicoCriada;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Traduz os eventos de dominio da ordem de servico em metricas do Micrometer.
 *
 * <p>Toda a instrumentacao de negocio vive aqui, e nao nos casos de uso: os sete servicos que
 * mudam o ciclo de vida da OS ja publicavam {@link StatusOrdemDeServicoAlterado}, entao um unico
 * listener cobre o fluxo inteiro sem que nenhum deles conheca Micrometer ou Datadog.
 *
 * <p>Metricas produzidas:
 *
 * <ul>
 *   <li>{@code os.criadas} - volume de OS abertas. Alimenta o "volume diario de OS".</li>
 *   <li>{@code os.transicoes} - transicoes por par (origem, destino).</li>
 *   <li>{@code os.tempo_na_situacao} - quanto tempo a OS ficou em cada situacao antes de sair
 *       dela. Alimenta o "tempo medio de execucao por status".</li>
 * </ul>
 *
 * <p>Os nomes NAO trazem o prefixo {@code oficina.} de proposito. O check OpenMetrics do Datadog
 * Agent exige um {@code namespace} e o prepende a tudo que raspa (ver a anotacao
 * {@code ad.datadoghq.com/oficina-api.checks} em {@code k8s/base/deployment.yaml}), entao o
 * prefixo aqui produziria {@code oficina.oficina.os.criadas} no Datadog. O nome final la e
 * {@code oficina.os.criadas}; em {@code /actuator/prometheus} sai como {@code os_criadas_total}.
 *
 * <p>Cardinalidade: as tags saem sempre de {@code SituacaoOrdemDeServico} (seis valores fechados).
 * Numero de OS e id de cliente NAO viram tag - eles vao para o MDC dos logs, onde a busca por
 * ordem especifica e barata, em vez de explodir series temporais.
 */
@Component
public class MetricasOrdemServicoListener {
    private static final Logger log = LoggerFactory.getLogger(MetricasOrdemServicoListener.class);

    static final String CONTADOR_CRIADAS = "os.criadas";
    static final String CONTADOR_TRANSICOES = "os.transicoes";
    static final String TEMPO_NA_SITUACAO = "os.tempo_na_situacao";

    private final MeterRegistry meterRegistry;

    public MetricasOrdemServicoListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void aoCriar(OrdemDeServicoCriada evento) {
        meterRegistry.counter(CONTADOR_CRIADAS).increment();
    }

    @EventListener
    public void aoMudarSituacao(StatusOrdemDeServicoAlterado evento) {
        meterRegistry.counter(
                        CONTADOR_TRANSICOES,
                        "situacao_anterior", evento.situacaoAnterior().name(),
                        "situacao", evento.novaSituacao().name())
                .increment();

        Duration permanencia = evento.duracaoNaSituacaoAnterior();
        if (permanencia == null) {
            // Ordem anterior a migration V19 sem timestamp para o backfill: contamos a transicao,
            // mas nao inventamos duracao - um zero aqui puxaria a media da etapa para baixo.
            log.debug("Transicao sem duracao mensuravel; timer ignorado. numeroOrdemServico={}, situacaoAnterior={}",
                    evento.numeroOrdemServico(), evento.situacaoAnterior());
            return;
        }
        meterRegistry.timer(TEMPO_NA_SITUACAO, "situacao", evento.situacaoAnterior().name())
                .record(permanencia);
    }
}
