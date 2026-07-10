package br.com.oficina.notificacao.domain.model;

/**
 * Estado de uma {@link Notificacao} no padrão outbox (spec 007).
 * <ul>
 *   <li>{@code PENDENTE} — registrada, ainda elegível a (re)processamento enquanto não esgotar as tentativas;</li>
 *   <li>{@code ENVIADA} — entregue com sucesso (terminal, nunca reenviada);</li>
 *   <li>{@code FALHOU} — terminal: esgotou as tentativas ou é não-enviável (sem e-mail / cliente inexistente).</li>
 * </ul>
 */
public enum StatusNotificacao {
    PENDENTE,
    ENVIADA,
    FALHOU
}
