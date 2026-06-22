package br.com.oficina.notificacao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServicoAlterado;

@Service
public class EnviarNotificacaoStatusOSService {
    private static final Logger log = LoggerFactory.getLogger(EnviarNotificacaoStatusOSService.class);

    private final ClienteRepository clienteRepository;
    private final NotificadorEmail notificadorEmail;

    public EnviarNotificacaoStatusOSService(ClienteRepository clienteRepository, NotificadorEmail notificadorEmail) {
        this.clienteRepository = clienteRepository;
        this.notificadorEmail = notificadorEmail;
    }

    @Async
    @EventListener
    public void enviar(StatusOrdemDeServicoAlterado evento) {
        try {
            clienteRepository.buscarPorId(evento.clienteId())
                    .ifPresentOrElse(
                            cliente -> enviarSePossuirEmail(cliente, evento),
                            () -> log.warn("Cliente da notificacao de OS nao encontrado. clienteId={}, numeroOrdemServico={}",
                                    evento.clienteId(),
                                    evento.numeroOrdemServico()));
        } catch (RuntimeException exception) {
            log.warn("Falha ao notificar mudanca de status da OS. numeroOrdemServico={}, novaSituacao={}",
                    evento.numeroOrdemServico(),
                    evento.novaSituacao().getDescricao(),
                    exception);
        }
    }

    private void enviarSePossuirEmail(Cliente cliente, StatusOrdemDeServicoAlterado evento) {
        if (cliente.getEmail() == null) {
            log.info("Cliente sem e-mail para notificacao de OS. clienteId={}, numeroOrdemServico={}",
                    cliente.getId(),
                    evento.numeroOrdemServico());
            return;
        }

        notificadorEmail.enviar(
                cliente.getEmail(),
                "Atualizacao da ordem de servico " + evento.numeroOrdemServico(),
                "Sua ordem de servico " + evento.numeroOrdemServico()
                        + " agora esta em: " + evento.novaSituacao().getDescricao() + ".");
        log.info("Notificacao de OS enviada. clienteId={}, numeroOrdemServico={}, novaSituacao={}",
                cliente.getId(),
                evento.numeroOrdemServico(),
                evento.novaSituacao().getDescricao());
    }
}
