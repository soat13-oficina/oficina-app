package br.com.oficina.common.infrastructure.observabilidade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Aplica {@code ambiente} como tag comum a todas as metricas.
 *
 * <p>Feito em codigo, e nao por {@code management.metrics.tags.*}, para que a tag seja garantida:
 * propriedade de configuracao desconhecida nao falha o boot no Spring, entao um nome errado
 * passaria despercebido ate alguem reparar que o dashboard mistura homologacao com producao.
 */
@Configuration
public class MetricasCommonTagsConfig {

    /**
     * O default aqui nao e redundante com o de {@code application.yml}: os
     * {@code application.yml} de teste sombreiam o principal no classpath, entao sem ele a
     * resolucao do placeholder falha e derruba o contexto inteiro do Spring nos testes.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> ambienteCommonTag(
            @Value("${observabilidade.ambiente:local}") String ambiente) {
        return registry -> registry.config().commonTags("ambiente", ambiente);
    }
}
