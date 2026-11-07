package br.com.oficina.common.infrastructure.mensageria;

import org.springframework.stereotype.Component;

/**
 * Entrega dentro do processo: "enviar" e passar direto ao {@link ConsumidorMensagens}, na thread do
 * publicador do outbox. Usado para as filas cujos participantes ainda moram no monolito (ver
 * {@link TransportePorFila}).
 *
 * <p>Mesmo em memoria a mensagem passa pelo outbox, pela serializacao do envelope e pela
 * idempotencia do consumidor - o caminho e o mesmo que o SQS percorre, so sem rede.
 */
@Component
public class TransporteEmMemoria implements TransporteMensagens {

    private final ConsumidorMensagens consumidor;

    public TransporteEmMemoria(ConsumidorMensagens consumidor) {
        this.consumidor = consumidor;
    }

    @Override
    public void enviar(MensagemSaida mensagem) {
        consumidor.consumir(mensagem.corpo());
    }
}
