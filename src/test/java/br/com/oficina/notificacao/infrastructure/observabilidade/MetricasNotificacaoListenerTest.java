package br.com.oficina.notificacao.infrastructure.observabilidade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.notificacao.domain.model.NotificacaoFalhouDefinitivamente;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MetricasNotificacaoListenerTest {

    @Test
    void deveContarFalhasDefinitivasDeNotificacao() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MetricasNotificacaoListener listener = new MetricasNotificacaoListener(registry);

        listener.aoFalharDefinitivamente(new NotificacaoFalhouDefinitivamente(
                UUID.randomUUID(), UUID.randomUUID(), "OS-001", 5, LocalDateTime.now()));

        assertEquals(1.0, registry.counter("notificacoes.falhas").count());
    }
}
