package br.com.oficina.config;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Carrega o MDC da thread que publicou o evento para a thread do pool que o processa.
     *
     * <p>Sem isso o envio de notificacao - que e {@code @Async} - loga sem
     * {@code correlation_id} nem {@code numero_ordem_servico}, e o rastro da requisicao morre
     * exatamente no ponto assincrono, que e onde mais se precisa dele para investigar entrega
     * falha.
     *
     * <p>Basta expor o bean: o {@code TaskExecutionAutoConfiguration} do Boot injeta um
     * {@code TaskDecorator} unico no executor padrao, sem que precisemos redefinir o pool.
     */
    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextoDeOrigem = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> contextoPreexistente = MDC.getCopyOfContextMap();
                if (contextoDeOrigem == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(contextoDeOrigem);
                }
                try {
                    runnable.run();
                } finally {
                    // Threads do pool sao reaproveitadas: restaura o estado anterior para nao
                    // vazar o contexto de uma requisicao para a proxima tarefa da mesma thread.
                    if (contextoPreexistente == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(contextoPreexistente);
                    }
                }
            };
        };
    }
}
