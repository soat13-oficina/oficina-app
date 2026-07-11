package br.com.oficina.notificacao.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import br.com.oficina.notificacao.domain.model.Notificacao;
import br.com.oficina.notificacao.domain.model.StatusNotificacao;
import br.com.oficina.notificacao.domain.repository.NotificacaoRepository;

@SpringBootTest
class JpaNotificacaoRepositoryIntegrationTest {

    private static final int MAX_TENTATIVAS = 3;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private SpringDataNotificacaoRepository springDataNotificacaoRepository;

    @BeforeEach
    void setUp() {
        springDataNotificacaoRepository.deleteAll();
    }

    @Test
    void devePersistirERecuperarStatusETentativas() {
        Notificacao notificacao = Notificacao.criarPendente(
                UUID.randomUUID(), "OS-RT-1", "dest@email.com", "Assunto", "Corpo", "Execução",
                LocalDateTime.now().minusMinutes(10));
        notificacao.registrarFalha(MAX_TENTATIVAS, LocalDateTime.now().minusMinutes(5));
        notificacaoRepository.salvar(notificacao);

        Notificacao recarregada = notificacaoRepository.buscarPorId(notificacao.getId()).orElseThrow();
        assertEquals(StatusNotificacao.PENDENTE, recarregada.getStatus());
        assertEquals(1, recarregada.getTentativas());
        assertEquals("dest@email.com", recarregada.getDestinatarioEmail());
        assertEquals("OS-RT-1", recarregada.getNumeroOrdemServico());
    }

    @Test
    void deveAtualizarMesmaLinhaAoRemarcarStatus() {
        Notificacao notificacao = Notificacao.criarPendente(
                UUID.randomUUID(), "OS-RT-2", "dest@email.com", "Assunto", "Corpo", "Execução",
                LocalDateTime.now().minusMinutes(10));
        notificacaoRepository.salvar(notificacao);
        notificacao.marcarEnviada(LocalDateTime.now());
        notificacaoRepository.salvar(notificacao);

        assertEquals(1, springDataNotificacaoRepository.count());
        assertEquals(StatusNotificacao.ENVIADA,
                notificacaoRepository.buscarPorId(notificacao.getId()).orElseThrow().getStatus());
    }

    @Test
    void buscarReprocessaveisDeveRetornarApenasPendentesElegiveis() {
        LocalDateTime antigo = LocalDateTime.now().minusMinutes(10);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(1);

        Notificacao elegivel = pendente("OS-EL", antigo);
        Notificacao recente = pendente("OS-REC", LocalDateTime.now());

        Notificacao enviada = pendente("OS-ENV", antigo);
        enviada.marcarEnviada(antigo);

        Notificacao falhou = pendente("OS-FAL", antigo);
        falhou.marcarNaoEnviavel(antigo);

        notificacaoRepository.salvar(elegivel);
        notificacaoRepository.salvar(recente);
        notificacaoRepository.salvar(enviada);
        notificacaoRepository.salvar(falhou);

        List<Notificacao> reprocessaveis = notificacaoRepository.buscarReprocessaveis(MAX_TENTATIVAS, cutoff);

        assertEquals(1, reprocessaveis.size());
        assertEquals(elegivel.getId(), reprocessaveis.get(0).getId());
        assertTrue(reprocessaveis.stream().allMatch(n -> n.getStatus() == StatusNotificacao.PENDENTE));
    }

    private static Notificacao pendente(String numeroOrdemServico, LocalDateTime criadoEm) {
        return Notificacao.criarPendente(
                UUID.randomUUID(), numeroOrdemServico, "dest@email.com",
                "Assunto", "Corpo", "Execução", criadoEm);
    }
}
