package br.com.oficina.ordemservico.saga.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Tipos e payloads das mensagens que o orquestrador troca, como em {@code docs/contratos/mensagens.md}.
 *
 * <p>Copia propria do OS Service: quando o Billing e a Execution forem extraidos, cada servico
 * carrega a sua versao do contrato - o que os une e o JSON Schema, nao uma classe compartilhada.
 */
public final class MensagensSaga {

    // Comandos enviados pelo orquestrador
    public static final String CANCELAR_ORCAMENTO = "CancelarOrcamento";

    // Eventos recebidos
    public static final String ORCAMENTO_APROVADO = "OrcamentoAprovado";
    public static final String ORCAMENTO_REJEITADO = "OrcamentoRejeitado";
    public static final String ORCAMENTO_CANCELADO = "OrcamentoCancelado";

    private MensagensSaga() {
    }

    public record CancelarOrcamento(UUID orcamentoId, String motivo) {
    }

    public record OrcamentoDecidido(UUID orcamentoId, String canal, Instant decididoEm) {
    }

    public record OrcamentoCancelado(UUID orcamentoId, boolean pagamentoCancelado) {
    }
}
