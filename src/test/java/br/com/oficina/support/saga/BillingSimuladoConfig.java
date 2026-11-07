package br.com.oficina.support.saga;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;

/**
 * Nos testes com contexto Spring, quem atende {@code billing-commands} e o {@link BillingSimulado} e quem
 * atende {@code execution-commands} e o {@link ExecutionSimulado}: as filas nao estao ligadas ao SQS e os
 * servicos de verdade sao outros. O cadastro de pecas tambem e simulado ({@link CatalogoSimulado}) e
 * substitui a consulta REST a Execution.
 */
@Configuration
public class BillingSimuladoConfig {

    @Bean
    @Primary
    public CatalogoSimulado catalogoSimulado() {
        return new CatalogoSimulado();
    }

    @Bean
    public BillingSimulado billingSimulado(PublicadorMensagens publicador, FabricaEnvelopes fabrica,
                                           TransactionTemplate transacao, CatalogoSimulado catalogo) {
        return new BillingSimulado(publicador, fabrica, transacao, catalogo);
    }

    @Bean
    public ExecutionSimulado executionSimulado(PublicadorMensagens publicador, FabricaEnvelopes fabrica,
                                               TransactionTemplate transacao, CatalogoSimulado catalogo) {
        return new ExecutionSimulado(publicador, fabrica, transacao, catalogo);
    }
}
