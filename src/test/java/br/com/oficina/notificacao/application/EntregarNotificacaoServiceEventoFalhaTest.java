package br.com.oficina.notificacao.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.oficina.notificacao.domain.model.Notificacao;
import br.com.oficina.notificacao.domain.model.NotificacaoFalhouDefinitivamente;
import br.com.oficina.notificacao.domain.model.StatusNotificacao;
import br.com.oficina.support.persistence.TestNotificacaoRepository;

/**
 * O evento existe para alimentar o monitor de falha de entrega. O ponto delicado é ele sair
 * APENAS no estado terminal: uma falha transiente ainda será reprocessada e alertar nela seria
 * ruído.
 */
class EntregarNotificacaoServiceEventoFalhaTest {

    private static final int MAX_TENTATIVAS = 3;

    @Test
    void devePublicarEventoQuandoNotificacaoEsgotaAsTentativas() {
        TestNotificacaoRepository repositorio = new TestNotificacaoRepository();
        List<Object> eventos = new ArrayList<>();
        EntregarNotificacaoService service = new EntregarNotificacaoService(
                notificadorQueFalha(), repositorio, MAX_TENTATIVAS, eventos::add);

        Notificacao notificacao = pendente();
        // Duas falhas já registradas: a tentativa desta chamada é a que atinge o limite.
        notificacao.registrarFalha(MAX_TENTATIVAS, LocalDateTime.now());
        notificacao.registrarFalha(MAX_TENTATIVAS, LocalDateTime.now());
        repositorio.salvar(notificacao);

        service.entregar(notificacao);

        assertEquals(StatusNotificacao.FALHOU, notificacao.getStatus());
        assertEquals(1, eventos.size());
        NotificacaoFalhouDefinitivamente evento =
                assertInstanceOf(NotificacaoFalhouDefinitivamente.class, eventos.get(0));
        assertEquals(MAX_TENTATIVAS, evento.tentativas());
        assertEquals("OS-001", evento.numeroOrdemServico());
    }

    @Test
    void naoDevePublicarEventoEmFalhaTransiente() {
        TestNotificacaoRepository repositorio = new TestNotificacaoRepository();
        List<Object> eventos = new ArrayList<>();
        EntregarNotificacaoService service = new EntregarNotificacaoService(
                notificadorQueFalha(), repositorio, MAX_TENTATIVAS, eventos::add);

        Notificacao notificacao = pendente();
        repositorio.salvar(notificacao);

        service.entregar(notificacao);

        assertEquals(StatusNotificacao.PENDENTE, notificacao.getStatus());
        assertTrue(eventos.isEmpty());
    }

    @Test
    void naoDevePublicarEventoQuandoEntregaDaCerto() {
        TestNotificacaoRepository repositorio = new TestNotificacaoRepository();
        List<Object> eventos = new ArrayList<>();
        EntregarNotificacaoService service = new EntregarNotificacaoService(
                (destinatario, assunto, corpo) -> {
                }, repositorio, MAX_TENTATIVAS, eventos::add);

        Notificacao notificacao = pendente();
        repositorio.salvar(notificacao);

        service.entregar(notificacao);

        assertEquals(StatusNotificacao.ENVIADA, notificacao.getStatus());
        assertTrue(eventos.isEmpty());
    }

    private static NotificadorEmail notificadorQueFalha() {
        return (destinatario, assunto, corpo) -> {
            throw new IllegalStateException("Falha simulada de envio.");
        };
    }

    private static Notificacao pendente() {
        return Notificacao.criarPendente(
                UUID.randomUUID(),
                "OS-001",
                "cliente@exemplo.com",
                "Atualizacao da ordem de servico OS-001",
                "Corpo",
                "Execucao",
                LocalDateTime.now());
    }
}
