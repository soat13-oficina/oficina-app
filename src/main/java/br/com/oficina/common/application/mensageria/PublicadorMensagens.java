package br.com.oficina.common.application.mensageria;

/**
 * Porta de saida para comandos e eventos da Saga.
 *
 * <p>Publicar NAO envia nada na hora: grava a mensagem no outbox na transacao corrente, que
 * precisa existir. A mensagem so sai se a transacao confirmar - e sai mesmo que o pod caia logo
 * depois, porque o publicador do outbox a encontra pendente no proximo ciclo (ADR 0005).
 */
public interface PublicadorMensagens {

    /**
     * @param fila nome logico da fila de destino (ver {@link Filas})
     */
    void publicar(String fila, Envelope envelope);
}
