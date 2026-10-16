package br.com.oficina.ordemservico.saga.domain.model;

/**
 * Comando que a Saga decidiu enviar a um participante. O agregado so decide; quem monta o
 * envelope e grava no outbox e o orquestrador.
 *
 * @param fila    nome logico da fila de destino
 * @param payload objeto serializado como payload do envelope (contrato em docs/contratos)
 */
public record ComandoSaga(String tipo, String fila, Object payload) {
}
