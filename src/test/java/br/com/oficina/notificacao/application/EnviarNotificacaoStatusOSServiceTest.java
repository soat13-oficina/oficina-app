package br.com.oficina.notificacao.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.domain.model.SituacaoOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;
import br.com.oficina.support.persistence.TestClienteRepository;

class EnviarNotificacaoStatusOSServiceTest {
    private TestClienteRepository clienteRepository;
    private NotificadorEmailFake notificadorEmail;
    private EnviarNotificacaoStatusOSService service;

    @BeforeEach
    void setUp() {
        clienteRepository = new TestClienteRepository();
        notificadorEmail = new NotificadorEmailFake();
        service = new EnviarNotificacaoStatusOSService(clienteRepository, notificadorEmail);
    }

    @Test
    void deveEnviarEmailQuandoClientePossuiEmail() {
        UUID clienteId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        clienteRepository.salvar(Cliente.reconstituir(
                clienteId,
                "Maria",
                "12345678901",
                TipoCliente.PF,
                "maria@email.com"));

        service.enviar(evento(clienteId));

        assertEquals("maria@email.com", notificadorEmail.destinatario);
        assertEquals("Atualizacao da ordem de servico OS-001", notificadorEmail.assunto);
        assertEquals("Sua ordem de servico OS-001 agora esta em: Execução.", notificadorEmail.corpo);
    }

    @Test
    void deveIgnorarQuandoClienteNaoPossuiEmail() {
        UUID clienteId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        clienteRepository.salvar(Cliente.reconstituir(
                clienteId,
                "Joao",
                "12345678901",
                TipoCliente.PF));

        service.enviar(evento(clienteId));

        assertEquals(0, notificadorEmail.quantidadeEnvios);
    }

    @Test
    void deveLogarQuandoClienteNaoEncontrado() {
        UUID clienteIdInexistente = UUID.fromString("99999999-9999-9999-9999-999999999999");

        assertDoesNotThrow(() -> service.enviar(evento(clienteIdInexistente)));

        assertEquals(0, notificadorEmail.quantidadeEnvios);
    }

    @Test
    void deveNaoPropagarFalhaDeEnvio() {
        UUID clienteId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        clienteRepository.salvar(Cliente.reconstituir(
                clienteId,
                "Ana",
                "12345678901",
                TipoCliente.PF,
                "ana@email.com"));
        notificadorEmail.falhar = true;

        assertDoesNotThrow(() -> service.enviar(evento(clienteId)));
        assertEquals(1, notificadorEmail.quantidadeEnvios);
    }

    private static StatusOrdemDeServicoAlterado evento(UUID clienteId) {
        return new StatusOrdemDeServicoAlterado(
                "OS-001",
                clienteId,
                SituacaoOrdemDeServico.AGUARDANDO_APROVACAO,
                SituacaoOrdemDeServico.EXECUCAO,
                LocalDateTime.of(2026, 6, 17, 10, 0));
    }

    private static class NotificadorEmailFake implements NotificadorEmail {
        private int quantidadeEnvios;
        private boolean falhar;
        private String destinatario;
        private String assunto;
        private String corpo;

        @Override
        public void enviar(String destinatario, String assunto, String corpo) {
            quantidadeEnvios++;
            if (falhar) {
                throw new IllegalStateException("SMTP indisponivel");
            }
            this.destinatario = destinatario;
            this.assunto = assunto;
            this.corpo = corpo;
        }
    }
}
