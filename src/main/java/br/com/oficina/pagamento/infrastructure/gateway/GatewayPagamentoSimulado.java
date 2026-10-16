package br.com.oficina.pagamento.infrastructure.gateway;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.oficina.pagamento.application.port.GatewayPagamento;
import br.com.oficina.pagamento.domain.model.Pagamento;

/**
 * Gateway de pagamento simulado (Fase 4, Passo 1): nao fala com ninguem. A cobranca ganha um link
 * ficticio e o "pagamento" acontece por {@code POST /pagamentos/{id}/simulacao}, que faz o papel do
 * webhook do Mercado Pago. Estornos sempre dao certo. Sai no Passo 3, junto com a extracao do Billing.
 */
@Component
public class GatewayPagamentoSimulado implements GatewayPagamento {
    private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoSimulado.class);

    private final String linkBase;

    public GatewayPagamentoSimulado(@Value("${pagamento.simulado.link-base:https://pagamento-simulado.oficina.local/checkout}") String linkBase) {
        this.linkBase = linkBase;
    }

    @Override
    public Cobranca criarCobranca(Pagamento pagamento, String descricao) {
        String referencia = "SIM-PREF-" + pagamento.getId();
        log.info("Cobranca simulada criada. pagamentoId={}, valor={}, descricao={}", pagamento.getId(), pagamento.getValor(), descricao);
        return new Cobranca(referencia, linkBase + "/" + pagamento.getId());
    }

    @Override
    public Estorno estornar(Pagamento pagamento) {
        String referencia = "SIM-REFUND-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Estorno simulado realizado. pagamentoId={}, valor={}, estorno={}", pagamento.getId(), pagamento.getValor(), referencia);
        return new Estorno(true, referencia, null);
    }
}
