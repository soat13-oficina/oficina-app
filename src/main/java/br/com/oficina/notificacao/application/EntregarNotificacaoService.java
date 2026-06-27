package br.com.oficina.notificacao.application;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.oficina.notificacao.domain.model.Notificacao;
import br.com.oficina.notificacao.domain.model.StatusNotificacao;
import br.com.oficina.notificacao.domain.repository.NotificacaoRepository;

/**
 * Executa a tentativa de entrega de uma {@link Notificacao} já registrada e persiste o resultado.
 * Compartilhado pelo listener (1ª tentativa) e pelo job de reprocessamento.
 */
@Service
public class EntregarNotificacaoService {
    private static final Logger log = LoggerFactory.getLogger(EntregarNotificacaoService.class);

    private final NotificadorEmail notificadorEmail;
    private final NotificacaoRepository notificacaoRepository;
    private final int maxTentativas;

    public EntregarNotificacaoService(
            NotificadorEmail notificadorEmail,
            NotificacaoRepository notificacaoRepository,
            @Value("${notificacao.reprocessamento.max-tentativas}") int maxTentativas) {
        this.notificadorEmail = notificadorEmail;
        this.notificacaoRepository = notificacaoRepository;
        this.maxTentativas = maxTentativas;
    }

    public void entregar(Notificacao notificacao) {
        try {
            notificadorEmail.enviar(
                    notificacao.getDestinatarioEmail(),
                    notificacao.getAssunto(),
                    notificacao.getCorpo());
            notificacao.marcarEnviada(LocalDateTime.now());
            log.info("Notificacao enviada. id={}, numeroOrdemServico={}",
                    notificacao.getId(), notificacao.getNumeroOrdemServico());
        } catch (RuntimeException exception) {
            notificacao.registrarFalha(maxTentativas, LocalDateTime.now());
            if (notificacao.getStatus() == StatusNotificacao.FALHOU) {
                log.warn("Notificacao falhou em definitivo apos esgotar tentativas. id={}, tentativas={}",
                        notificacao.getId(), notificacao.getTentativas(), exception);
            } else {
                log.warn("Falha transiente ao enviar notificacao; sera reprocessada. id={}, tentativas={}",
                        notificacao.getId(), notificacao.getTentativas());
            }
        }
        notificacaoRepository.salvar(notificacao);
    }
}
