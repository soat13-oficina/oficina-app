package br.com.oficina.notificacao.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

import br.com.oficina.notificacao.application.NotificadorEmail;

@Component
@Profile("ses")
public class NotificadorEmailSes implements NotificadorEmail {

    private static final Logger log = LoggerFactory.getLogger(NotificadorEmailSes.class);

    private final SesClient sesClient;
    private final String remetente;

    public NotificadorEmailSes(SesClient sesClient, AwsSesProperties properties) {
        this.sesClient = sesClient;
        this.remetente = properties.getRemetente();
    }

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(remetente)
                .destination(Destination.builder().toAddresses(destinatario).build())
                .message(Message.builder()
                        .subject(Content.builder().data(assunto).build())
                        .body(Body.builder().text(Content.builder().data(corpo).build()).build())
                        .build())
                .build();

        try {
            sesClient.sendEmail(request);
        } catch (SesException e) {
            String motivo = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            log.error("Falha ao enviar e-mail via Amazon SES para {}: {}", destinatario, motivo, e);
            throw new RuntimeException("Falha ao enviar e-mail via Amazon SES para " + destinatario, e);
        }
    }
}
