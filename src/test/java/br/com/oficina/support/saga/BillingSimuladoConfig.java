package br.com.oficina.support.saga;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.oficina.common.application.mensageria.FabricaEnvelopes;
import br.com.oficina.common.application.mensageria.PublicadorMensagens;
import br.com.oficina.pecainsumo.domain.repository.PecaInsumoRepository;

/**
 * Nos testes com contexto Spring, quem atende {@code billing-commands} e o {@link BillingSimulado}:
 * a fila nao esta ligada ao SQS e o Billing de verdade e outro servico.
 */
@Configuration
public class BillingSimuladoConfig {

    @Bean
    public BillingSimulado billingSimulado(PublicadorMensagens publicador, FabricaEnvelopes fabrica,
                                           TransactionTemplate transacao, PecaInsumoRepository pecas) {
        return new BillingSimulado(publicador, fabrica, transacao, pecas);
    }
}
