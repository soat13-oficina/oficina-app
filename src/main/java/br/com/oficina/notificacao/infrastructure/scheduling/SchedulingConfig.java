package br.com.oficina.notificacao.infrastructure.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o agendamento de tarefas (job de reprocessamento de notificações — spec 007).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
