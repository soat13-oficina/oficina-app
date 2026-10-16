package br.com.oficina.common.infrastructure.mensageria;

/**
 * Entrega fisica de uma mensagem ja confirmada no outbox. No Passo 1 da Fase 4 o transporte e em
 * memoria (tudo dentro do monolito); a partir do Passo 3 e o SQS FIFO. O publicador do outbox e o
 * resto da aplicacao nao sabem qual esta ativo.
 *
 * <p>Lancar significa "nao entregue": o outbox registra a falha e tenta de novo no proximo ciclo.
 */
public interface TransporteMensagens {

    void enviar(MensagemSaida mensagem);

    /**
     * @param fila      nome logico (ver {@code Filas})
     * @param groupId   ordemServicoId - ordem garantida dentro do grupo
     * @param corpo     envelope serializado
     */
    record MensagemSaida(String fila, String groupId, String messageId, String tipo, String corpo) {
    }
}
