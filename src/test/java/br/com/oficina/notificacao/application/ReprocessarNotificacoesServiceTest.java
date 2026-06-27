package br.com.oficina.notificacao.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.oficina.notificacao.domain.model.Notificacao;
import br.com.oficina.notificacao.domain.model.StatusNotificacao;
import br.com.oficina.support.persistence.TestNotificacaoRepository;

class ReprocessarNotificacoesServiceTest {
    private static final int MAX_TENTATIVAS = 3;
    private static final int CARENCIA_SEGUNDOS = 60;

    private TestNotificacaoRepository notificacaoRepository;
    private NotificadorEmailFake notificadorEmail;
    private ReprocessarNotificacoesService service;

    @BeforeEach
    void setUp() {
        notificacaoRepository = new TestNotificacaoRepository();
        notificadorEmail = new NotificadorEmailFake();
        EntregarNotificacaoService entregar =
                new EntregarNotificacaoService(notificadorEmail, notificacaoRepository, MAX_TENTATIVAS);
        service = new ReprocessarNotificacoesService(
                notificacaoRepository, entregar, MAX_TENTATIVAS, CARENCIA_SEGUNDOS);
    }

    @Test
    void deveEntregarPendenteForaDaCarencia() {
        Notificacao notificacao = pendente("OS-1", agoraMenos(120));
        notificacaoRepository.salvar(notificacao);

        service.reprocessar();

        assertEquals(1, notificadorEmail.quantidadeEnvios);
        assertEquals(StatusNotificacao.ENVIADA, recarregar(notificacao).getStatus());
    }

    @Test
    void naoDeveReenviarNotificacaoJaEnviada() {
        Notificacao notificacao = pendente("OS-2", agoraMenos(120));
        notificacao.marcarEnviada(agoraMenos(120));
        notificacaoRepository.salvar(notificacao);

        service.reprocessar();

        assertEquals(0, notificadorEmail.quantidadeEnvios);
        assertEquals(StatusNotificacao.ENVIADA, recarregar(notificacao).getStatus());
    }

    @Test
    void deveMarcarFalhouAoEsgotarTentativas() {
        Notificacao notificacao = pendente("OS-3", agoraMenos(120));
        notificacao.registrarFalha(MAX_TENTATIVAS, agoraMenos(120));
        notificacao.registrarFalha(MAX_TENTATIVAS, agoraMenos(120));
        notificacaoRepository.salvar(notificacao);
        notificadorEmail.falhar = true;

        service.reprocessar();

        assertEquals(1, notificadorEmail.quantidadeEnvios);
        Notificacao recarregada = recarregar(notificacao);
        assertEquals(StatusNotificacao.FALHOU, recarregada.getStatus());
        assertEquals(MAX_TENTATIVAS, recarregada.getTentativas());
    }

    @Test
    void naoDeveSelecionarPendenteRecenteDentroDaCarencia() {
        Notificacao notificacao = pendente("OS-4", LocalDateTime.now());
        notificacaoRepository.salvar(notificacao);

        service.reprocessar();

        assertEquals(0, notificadorEmail.quantidadeEnvios);
        assertEquals(StatusNotificacao.PENDENTE, recarregar(notificacao).getStatus());
    }

    private Notificacao recarregar(Notificacao notificacao) {
        return notificacaoRepository.buscarPorId(notificacao.getId()).orElseThrow();
    }

    private static Notificacao pendente(String numeroOrdemServico, LocalDateTime criadoEm) {
        return Notificacao.criarPendente(
                UUID.randomUUID(), numeroOrdemServico, "dest@email.com",
                "Atualizacao", "Corpo", "Execução", criadoEm);
    }

    private static LocalDateTime agoraMenos(int segundos) {
        return LocalDateTime.now().minusSeconds(segundos);
    }

    private static class NotificadorEmailFake implements NotificadorEmail {
        private int quantidadeEnvios;
        private boolean falhar;

        @Override
        public void enviar(String destinatario, String assunto, String corpo) {
            quantidadeEnvios++;
            if (falhar) {
                throw new IllegalStateException("SMTP indisponivel");
            }
        }
    }
}
