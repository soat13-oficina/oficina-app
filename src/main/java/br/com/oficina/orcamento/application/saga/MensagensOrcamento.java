package br.com.oficina.orcamento.application.saga;

import java.time.Instant;
import java.util.UUID;

/**
 * Tipos e payloads das mensagens da Saga do lado do orcamento (futuro Billing Service), como em
 * {@code docs/contratos/mensagens.md}. Copia propria do modulo: quando o Billing for extraido, leva
 * esta classe junto - o que o une ao OS Service e o contrato, nao uma classe compartilhada.
 */
public final class MensagensOrcamento {

    // Comandos recebidos do orquestrador
    public static final String CANCELAR_ORCAMENTO = "CancelarOrcamento";

    // Eventos publicados
    public static final String ORCAMENTO_APROVADO = "OrcamentoAprovado";
    public static final String ORCAMENTO_REJEITADO = "OrcamentoRejeitado";
    public static final String ORCAMENTO_CANCELADO = "OrcamentoCancelado";

    private MensagensOrcamento() {
    }

    public record CancelarOrcamento(UUID orcamentoId, String motivo) {
    }

    public record OrcamentoDecidido(UUID orcamentoId, String canal, Instant decididoEm) {
    }

    public record OrcamentoCancelado(UUID orcamentoId, boolean pagamentoCancelado) {
    }
}
