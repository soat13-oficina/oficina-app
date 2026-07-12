package br.com.oficina.notificacao.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.MessageRejectedException;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

class NotificadorEmailSesTest {

    private AwsSesProperties properties() {
        AwsSesProperties properties = new AwsSesProperties();
        properties.setRegion("us-east-1");
        properties.setRemetente("oficina@exemplo.com");
        return properties;
    }

    @Test
    void deveEnviarEmailComCamposCorretos() {
        SesClient sesClient = mock(SesClient.class);
        NotificadorEmailSes notificador = new NotificadorEmailSes(sesClient, properties());

        notificador.enviar("cliente@email.com", "Assunto da OS", "Corpo da mensagem");

        ArgumentCaptor<SendEmailRequest> captor = forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());
        SendEmailRequest request = captor.getValue();
        assertEquals("oficina@exemplo.com", request.source());
        assertEquals("cliente@email.com", request.destination().toAddresses().get(0));
        assertEquals("Assunto da OS", request.message().subject().data());
        assertEquals("Corpo da mensagem", request.message().body().text().data());
    }

    @Test
    void deveRelancarComoRuntimeExceptionQuandoSesFalhar() {
        SesClient sesClient = mock(SesClient.class);
        MessageRejectedException falha = MessageRejectedException.builder()
                .message("Endereço de e-mail inválido")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenThrow(falha);

        NotificadorEmailSes notificador = new NotificadorEmailSes(sesClient, properties());

        assertThrows(RuntimeException.class,
                () -> notificador.enviar("cliente@email.com", "Assunto", "Corpo"));
    }
}
