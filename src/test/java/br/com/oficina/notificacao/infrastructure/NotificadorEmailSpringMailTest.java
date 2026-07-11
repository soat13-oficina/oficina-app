package br.com.oficina.notificacao.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class NotificadorEmailSpringMailTest {

    @Test
    void deveEnviarEmailComCamposCorretos() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        NotificadorEmailSpringMail notificador = new NotificadorEmailSpringMail(mailSender);

        notificador.enviar("cliente@email.com", "Assunto da OS", "Corpo da mensagem");

        ArgumentCaptor<SimpleMailMessage> captor = forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();
        assertEquals("cliente@email.com", mensagem.getTo()[0]);
        assertEquals("Assunto da OS", mensagem.getSubject());
        assertEquals("Corpo da mensagem", mensagem.getText());
    }
}
