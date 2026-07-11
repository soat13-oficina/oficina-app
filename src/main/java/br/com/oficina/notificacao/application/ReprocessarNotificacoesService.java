package br.com.oficina.notificacao.application;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.oficina.notificacao.domain.model.Notificacao;
import br.com.oficina.notificacao.domain.repository.NotificacaoRepository;

/**
 * Reprocessa notificações pendentes ainda dentro do limite de tentativas e fora da janela de carência
 * (evita corrida com o envio ativo do listener). Acionado pelo job agendado (spec 007 / CRI-001).
 */
@Service
public class ReprocessarNotificacoesService {
    private static final Logger log = LoggerFactory.getLogger(ReprocessarNotificacoesService.class);

    private final NotificacaoRepository notificacaoRepository;
    private final EntregarNotificacaoService entregarNotificacaoService;
    private final int maxTentativas;
    private final int carenciaSegundos;

    public ReprocessarNotificacoesService(
            NotificacaoRepository notificacaoRepository,
            EntregarNotificacaoService entregarNotificacaoService,
            @Value("${notificacao.reprocessamento.max-tentativas}") int maxTentativas,
            @Value("${notificacao.reprocessamento.carencia-segundos}") int carenciaSegundos) {
        this.notificacaoRepository = notificacaoRepository;
        this.entregarNotificacaoService = entregarNotificacaoService;
        this.maxTentativas = maxTentativas;
        this.carenciaSegundos = carenciaSegundos;
    }

    public void reprocessar() {
        LocalDateTime anteriorA = LocalDateTime.now().minusSeconds(carenciaSegundos);
        List<Notificacao> reprocessaveis = notificacaoRepository.buscarReprocessaveis(maxTentativas, anteriorA);
        if (reprocessaveis.isEmpty()) {
            return;
        }
        log.info("Iniciando reprocessamento de notificacoes pendentes. quantidade={}", reprocessaveis.size());
        for (Notificacao notificacao : reprocessaveis) {
            entregarNotificacaoService.entregar(notificacao);
        }
        log.info("Reprocessamento de notificacoes concluido. quantidade={}", reprocessaveis.size());
    }
}
