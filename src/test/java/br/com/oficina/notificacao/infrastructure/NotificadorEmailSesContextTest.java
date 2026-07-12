package br.com.oficina.notificacao.infrastructure;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import software.amazon.awssdk.services.ses.SesClient;

import br.com.oficina.notificacao.application.NotificadorEmail;

@SpringBootTest(properties = {
        "spring.profiles.active=ses",
        "aws.ses.remetente=oficina@exemplo.com"
})
class NotificadorEmailSesContextTest {

    @MockitoBean
    private SesClient sesClient;

    @Autowired
    private NotificadorEmail notificadorEmail;

    @Test
    void deveResolverNotificadorEmailSesComoUnicoBeanQuandoProfileSesAtivo() {
        assertInstanceOf(NotificadorEmailSes.class, notificadorEmail);
    }
}
