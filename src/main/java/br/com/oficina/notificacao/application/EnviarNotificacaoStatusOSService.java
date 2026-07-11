package br.com.oficina.notificacao.application;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.notificacao.domain.model.Notificacao;
import br.com.oficina.notificacao.domain.repository.NotificacaoRepository;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;

/**
 * Reage ao evento de mudança de status de OS, registrando a notificação (padrão outbox) e tentando entregá-la.
 * Permanece assíncrono e isolado da transação de negócio que origina o evento (spec 007 / CRI-001, FR-008).
 */
@Service
public class EnviarNotificacaoStatusOSService {
    private static final Logger log = LoggerFactory.getLogger(EnviarNotificacaoStatusOSService.class);

    private final ClienteRepository clienteRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final EntregarNotificacaoService entregarNotificacaoService;

    public EnviarNotificacaoStatusOSService(
            ClienteRepository clienteRepository,
            NotificacaoRepository notificacaoRepository,
            EntregarNotificacaoService entregarNotificacaoService) {
        this.clienteRepository = clienteRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.entregarNotificacaoService = entregarNotificacaoService;
    }

    @Async
    @EventListener
    public void enviar(StatusOrdemDeServicoAlterado evento) {
        try {
            Cliente cliente = clienteRepository.buscarPorId(evento.clienteId()).orElse(null);
            String email = cliente != null ? cliente.getEmail() : null;

            Notificacao notificacao = Notificacao.criarPendente(
                    evento.clienteId(),
                    evento.numeroOrdemServico(),
                    email,
                    "Atualizacao da ordem de servico " + evento.numeroOrdemServico(),
                    "Sua ordem de servico " + evento.numeroOrdemServico()
                            + " agora esta em: " + evento.novaSituacao().getDescricao() + ".",
                    evento.novaSituacao().getDescricao(),
                    LocalDateTime.now());
            // FR-001: registra a notificação como PENDENTE antes da tentativa de envio (resistência a crash).
            notificacaoRepository.salvar(notificacao);

            if (cliente == null) {
                notificacao.marcarNaoEnviavel(LocalDateTime.now());
                notificacaoRepository.salvar(notificacao);
                log.warn("Cliente da notificacao nao encontrado; notificacao marcada nao-enviavel. clienteId={}, numeroOrdemServico={}",
                        evento.clienteId(), evento.numeroOrdemServico());
                return;
            }
            if (email == null) {
                notificacao.marcarNaoEnviavel(LocalDateTime.now());
                notificacaoRepository.salvar(notificacao);
                log.info("Cliente sem e-mail para notificacao de OS; marcada nao-enviavel. clienteId={}, numeroOrdemServico={}",
                        cliente.getId(), evento.numeroOrdemServico());
                return;
            }

            entregarNotificacaoService.entregar(notificacao);
        } catch (RuntimeException exception) {
            log.warn("Falha ao processar notificacao de mudanca de status. numeroOrdemServico={}, novaSituacao={}",
                    evento.numeroOrdemServico(),
                    evento.novaSituacao().getDescricao(),
                    exception);
        }
    }
}
