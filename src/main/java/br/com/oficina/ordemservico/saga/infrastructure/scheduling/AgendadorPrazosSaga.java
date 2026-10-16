package br.com.oficina.ordemservico.saga.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.oficina.ordemservico.saga.application.OrquestradorSagaOrdemServico;

/**
 * {@code SagaTimeoutScheduler} do ADR 0004: a cada {@code saga.prazos.intervalo-ms} (30 s) aplica os
 * prazos vencidos - decisao do cliente e resposta das compensacoes. Desligavel por
 * {@code saga.prazos.agendamento-habilitado=false} (os testes chamam o orquestrador direto).
 */
@Component
@ConditionalOnProperty(name = "saga.prazos.agendamento-habilitado", havingValue = "true", matchIfMissing = true)
public class AgendadorPrazosSaga {
    private static final Logger log = LoggerFactory.getLogger(AgendadorPrazosSaga.class);

    private final OrquestradorSagaOrdemServico orquestrador;

    public AgendadorPrazosSaga(OrquestradorSagaOrdemServico orquestrador) {
        this.orquestrador = orquestrador;
    }

    @Scheduled(fixedDelayString = "${saga.prazos.intervalo-ms:30000}")
    public void verificar() {
        try {
            orquestrador.processarPrazosVencidos();
        } catch (RuntimeException e) {
            log.error("Falha no ciclo de prazos da Saga. erro={}", e.toString());
        }
    }
}
