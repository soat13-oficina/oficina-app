package br.com.oficina.pagamento.application.port;

import br.com.oficina.pagamento.domain.model.Pagamento;

/**
 * Provedor de pagamento (ADR 0007). Passo 1: simulado ({@code GatewayPagamentoSimulado}); Passo 3:
 * Mercado Pago Checkout Pro (preferencia + webhook + estorno), com {@code idempotencyKey} da cobranca.
 */
public interface GatewayPagamento {

    /** Cria a cobranca e devolve o link que o cliente usa para pagar. */
    Cobranca criarCobranca(Pagamento pagamento, String descricao);

    /** Devolve o valor ao cliente. Recusa do provedor nao e excecao: vem em {@link Estorno#realizado()}. */
    Estorno estornar(Pagamento pagamento);

    /** @param referenciaExterna id da preferencia no provedor */
    record Cobranca(String referenciaExterna, String link) {
    }

    /** @param referenciaExterna id do estorno no provedor, quando realizado */
    record Estorno(boolean realizado, String referenciaExterna, String motivoFalha) {
    }
}
