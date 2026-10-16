package br.com.oficina.common.infrastructure.mensageria;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Transporte do Passo 1 da Fase 4: todos os participantes da Saga ainda vivem neste processo, entao
 * "enviar" e entregar direto ao {@link ConsumidorMensagens}, na thread do publicador do outbox.
 *
 * <p>Mesmo em memoria a mensagem passa pelo outbox, pela serializacao do envelope e pela
 * idempotencia do consumidor - o caminho e o mesmo que o SQS vai percorrer, so sem rede. Trocar
 * por SQS e trocar este bean ({@code mensageria.transporte=sqs}).
 */
@Component
@ConditionalOnProperty(name = "mensageria.transporte", havingValue = "memoria", matchIfMissing = true)
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
