package br.com.oficina.notificacao.infrastructure.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.oficina.notificacao.application.ReprocessarNotificacoesService;

/**
 * Aciona o reprocessamento de notificações pendentes em intervalos configuráveis. O scheduler padrão do
 * Spring é de thread única e `fixedDelay` não sobrepõe execuções (garantia parcial de não-duplicidade).
 * O `initialDelay` evita disparos durante a inicialização e a suíte de testes.
 */
@Component
public class ReprocessamentoNotificacoesScheduler {
    private final ReprocessarNotificacoesService reprocessarNotificacoesService;

    public ReprocessamentoNotificacoesScheduler(ReprocessarNotificacoesService reprocessarNotificacoesService) {
        this.reprocessarNotificacoesService = reprocessarNotificacoesService;
    }

    @Scheduled(
            fixedDelayString = "${notificacao.reprocessamento.intervalo}",
            initialDelayString = "${notificacao.reprocessamento.intervalo}")
    public void executar() {
        reprocessarNotificacoesService.reprocessar();
    }
}
