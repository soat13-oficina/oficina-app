package br.com.oficina.notificacao.application;

public interface NotificadorEmail {
    void enviar(String destinatario, String assunto, String corpo);
}
