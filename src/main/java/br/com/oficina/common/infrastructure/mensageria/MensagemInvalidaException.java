package br.com.oficina.common.infrastructure.mensageria;

/**
 * Mensagem que nunca vai ser processada, por mais que se tente: fora do contrato ou de tipo sem
 * manipulador. No SQS vai direto para a DLQ; no transporte em memoria esgota as tentativas.
 */
public class MensagemInvalidaException extends RuntimeException {

    public MensagemInvalidaException(String mensagem) {
        super(mensagem);
    }

    public MensagemInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
