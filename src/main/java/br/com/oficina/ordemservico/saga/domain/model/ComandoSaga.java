package br.com.oficina.ordemservico.saga.domain.model;

/**
 * Comando que a Saga decidiu enviar a um participante. O agregado so decide; quem monta o
 * envelope e grava no outbox e o orquestrador.
 *
 * @param fila    nome logico da fila de destino
 * @param payload objeto serializado como payload do envelope (contrato em docs/contratos); nulo num reenvio
 * @param reenvio {@code true} = reenviar o ultimo comando deste tipo ja enviado para a OS, com o mesmo
 *                payload e {@code messageId} novo (timeout de resposta, ADR 0004)
 */
public record ComandoSaga(String tipo, String fila, Object payload, boolean reenvio) {

    public ComandoSaga(String tipo, String fila, Object payload) {
        this(tipo, fila, payload, false);
    }

    public static ComandoSaga reenvio(String tipo, String fila) {
        return new ComandoSaga(tipo, fila, null, true);
    }
}
