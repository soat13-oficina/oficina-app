package br.com.oficina.notificacao.infrastructure.observabilidade;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.com.oficina.notificacao.domain.model.NotificacaoFalhouDefinitivamente;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Contador das notificacoes que morreram apos esgotar as tentativas.
 *
 * <p>{@code notificacoes.falhas} - {@code oficina.notificacoes.falhas} no Datadog, ja com o
 * namespace do check OpenMetrics - e a base do monitor de "falha no processamento de ordens de
 * servico": diferente de um 4xx de regra de negocio (uso normal da API, que nao acorda ninguem),
 * chegar aqui significa que um cliente ficou sem ser avisado e ninguem vai tentar de novo.
 */
@Component
public class MetricasNotificacaoListener {

    static final String CONTADOR_FALHAS = "notificacoes.falhas";

    private final MeterRegistry meterRegistry;

    public MetricasNotificacaoListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void aoFalharDefinitivamente(NotificacaoFalhouDefinitivamente evento) {
        meterRegistry.counter(CONTADOR_FALHAS).increment();
    }
}
