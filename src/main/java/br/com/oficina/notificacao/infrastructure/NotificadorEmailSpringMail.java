package br.com.oficina.notificacao.infrastructure;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import br.com.oficina.notificacao.application.NotificadorEmail;

@Component
public class NotificadorEmailSpringMail implements NotificadorEmail {
    private final JavaMailSender mailSender;

    public NotificadorEmailSpringMail(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto);
        mensagem.setText(corpo);
        mailSender.send(mensagem);
    }
}
