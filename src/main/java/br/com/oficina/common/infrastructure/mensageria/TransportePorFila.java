package br.com.oficina.common.infrastructure.mensageria;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import br.com.oficina.common.infrastructure.mensageria.sqs.PropriedadesSqs;
import br.com.oficina.common.infrastructure.mensageria.sqs.TransporteSqs;

/**
 * Transporte usado pelo publicador do outbox: as filas de {@code mensageria.sqs.envio} saem pelo
 * SQS; as demais sao entregues dentro do processo. E o que permite extrair um servico de cada vez
 * (Passo 3: Billing; Passo 4: Execution) sem que o orquestrador saiba onde esta cada participante.
 */
@Primary
@Component
public class TransportePorFila implements TransporteMensagens {

    private final TransporteEmMemoria memoria;
    private final TransporteSqs sqs;
    private final PropriedadesSqs propriedades;

    public TransportePorFila(TransporteEmMemoria memoria, ObjectProvider<TransporteSqs> sqs, PropriedadesSqs propriedades) {
        this.memoria = memoria;
        this.sqs = sqs.getIfAvailable();
        this.propriedades = propriedades;
        if (this.sqs == null && !propriedades.envio().isEmpty()) {
            throw new IllegalStateException("mensageria.sqs.envio=" + propriedades.envio()
                    + " exige mensageria.sqs.habilitado=true");
        }
    }

    @Override
    public void enviar(MensagemSaida mensagem) {
        if (propriedades.envio().contains(mensagem.fila())) {
            sqs.enviar(mensagem);
        } else {
            memoria.enviar(mensagem);
        }
    }
}
