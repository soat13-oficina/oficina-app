package br.com.oficina.notificacao.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;

class NotificacaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 26, 10, 0);

    @Test
    void deveNascerPendenteComZeroTentativas() {
        Notificacao notificacao = novaPendente();

        assertEquals(StatusNotificacao.PENDENTE, notificacao.getStatus());
        assertEquals(0, notificacao.getTentativas());
        assertTrue(notificacao.ehReprocessavel(5));
    }

    @Test
    void deveMarcarEnviadaRegistrandoTentativa() {
        Notificacao notificacao = novaPendente();

        notificacao.marcarEnviada(AGORA);

        assertEquals(StatusNotificacao.ENVIADA, notificacao.getStatus());
        assertEquals(1, notificacao.getTentativas());
        assertEquals(AGORA, notificacao.getUltimaTentativaEm());
        assertFalse(notificacao.ehReprocessavel(5));
    }

    @Test
    void devePermanecerPendenteAntesDoLimiteDeTentativas() {
        Notificacao notificacao = novaPendente();

        notificacao.registrarFalha(3, AGORA);

        assertEquals(StatusNotificacao.PENDENTE, notificacao.getStatus());
        assertEquals(1, notificacao.getTentativas());
        assertTrue(notificacao.ehReprocessavel(3));
    }

    @Test
    void deveFalharAoAtingirOLimiteDeTentativas() {
        Notificacao notificacao = novaPendente();

        notificacao.registrarFalha(2, AGORA);
        notificacao.registrarFalha(2, AGORA);

        assertEquals(StatusNotificacao.FALHOU, notificacao.getStatus());
        assertEquals(2, notificacao.getTentativas());
        assertFalse(notificacao.ehReprocessavel(2));
    }

    @Test
    void deveMarcarNaoEnviavelComoFalhaTerminalSemTentativas() {
        Notificacao notificacao = novaPendente();

        notificacao.marcarNaoEnviavel(AGORA);

        assertEquals(StatusNotificacao.FALHOU, notificacao.getStatus());
        assertEquals(0, notificacao.getTentativas());
        assertFalse(notificacao.ehReprocessavel(5));
    }

    @Test
    void naoDeveAtualizarNotificacaoJaTerminal() {
        Notificacao notificacao = novaPendente();
        notificacao.marcarEnviada(AGORA);

        assertThrows(RegraDeNegocioException.class, () -> notificacao.marcarEnviada(AGORA));
        assertThrows(RegraDeNegocioException.class, () -> notificacao.registrarFalha(5, AGORA));
    }

    private static Notificacao novaPendente() {
        return Notificacao.criarPendente(
                UUID.randomUUID(), "OS-001", "cliente@email.com",
                "Atualizacao da OS", "Sua OS mudou de status", "Execução", AGORA);
    }
}
