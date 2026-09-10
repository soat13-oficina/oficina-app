package br.com.oficina.notificacao.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notificacao que esgotou {@code notificacao.reprocessamento.max-tentativas} e nao sera mais
 * reprocessada - o cliente nao foi avisado da mudanca de status da OS dele.
 *
 * <p>Existe para dar um sinal alertavel a esta falha. E o unico estado terminal de erro do modulo
 * que representa perda real de entrega: falha transiente ainda tem retentativa pela frente, e
 * "nao-enviavel" (cliente sem e-mail) e cadastro incompleto, nao incidente.
 */
public record NotificacaoFalhouDefinitivamente(
        UUID notificacaoId,
        UUID clienteId,
        String numeroOrdemServico,
        int tentativas,
        LocalDateTime ocorridoEm) {

    public static NotificacaoFalhouDefinitivamente de(Notificacao notificacao) {
        return new NotificacaoFalhouDefinitivamente(
                notificacao.getId(),
                notificacao.getClienteId(),
                notificacao.getNumeroOrdemServico(),
                notificacao.getTentativas(),
                notificacao.getUltimaTentativaEm());
    }
}
