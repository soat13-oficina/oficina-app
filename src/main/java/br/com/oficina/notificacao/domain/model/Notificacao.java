package br.com.oficina.notificacao.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;

/**
 * Notificação de mudança de status de OS, persistida no padrão outbox (spec 007 / CRI-001).
 * Domínio puro (sem anotações JPA): o mapeamento vive em {@code NotificacaoJpaEntity}.
 */
public class Notificacao {
    private final UUID id;
    private final UUID clienteId;
    private final String numeroOrdemServico;
    private final String destinatarioEmail;
    private final String assunto;
    private final String corpo;
    private final String situacao;
    private final LocalDateTime criadoEm;

    private StatusNotificacao status;
    private int tentativas;
    private LocalDateTime ultimaTentativaEm;

    private Notificacao(
            UUID id,
            UUID clienteId,
            String numeroOrdemServico,
            String destinatarioEmail,
            String assunto,
            String corpo,
            String situacao,
            StatusNotificacao status,
            int tentativas,
            LocalDateTime criadoEm,
            LocalDateTime ultimaTentativaEm) {
        this.id = id;
        this.clienteId = clienteId;
        this.numeroOrdemServico = numeroOrdemServico;
        this.destinatarioEmail = destinatarioEmail;
        this.assunto = assunto;
        this.corpo = corpo;
        this.situacao = situacao;
        this.status = status;
        this.tentativas = tentativas;
        this.criadoEm = criadoEm;
        this.ultimaTentativaEm = ultimaTentativaEm;
    }

    /** Cria uma notificação PENDENTE (registro antes da primeira tentativa de envio). */
    public static Notificacao criarPendente(
            UUID clienteId,
            String numeroOrdemServico,
            String destinatarioEmail,
            String assunto,
            String corpo,
            String situacao,
            LocalDateTime criadoEm) {
        return new Notificacao(
                UUID.randomUUID(), clienteId, numeroOrdemServico, destinatarioEmail, assunto, corpo, situacao,
                StatusNotificacao.PENDENTE, 0, criadoEm, null);
    }

    /** Reconstrói uma notificação a partir da persistência. */
    public static Notificacao reconstituir(
            UUID id,
            UUID clienteId,
            String numeroOrdemServico,
            String destinatarioEmail,
            String assunto,
            String corpo,
            String situacao,
            StatusNotificacao status,
            int tentativas,
            LocalDateTime criadoEm,
            LocalDateTime ultimaTentativaEm) {
        return new Notificacao(
                id, clienteId, numeroOrdemServico, destinatarioEmail, assunto, corpo, situacao,
                status, tentativas, criadoEm, ultimaTentativaEm);
    }

    /** Marca a notificação como entregue. Registra a tentativa bem-sucedida. */
    public void marcarEnviada(LocalDateTime quando) {
        exigirPendente();
        this.tentativas++;
        this.ultimaTentativaEm = quando;
        this.status = StatusNotificacao.ENVIADA;
    }

    /**
     * Registra uma tentativa de envio falha. Incrementa as tentativas; ao atingir o limite, a notificação
     * torna-se {@code FALHOU} (terminal), caso contrário permanece {@code PENDENTE} (reprocessável).
     */
    public void registrarFalha(int maxTentativas, LocalDateTime quando) {
        exigirPendente();
        this.tentativas++;
        this.ultimaTentativaEm = quando;
        if (this.tentativas >= maxTentativas) {
            this.status = StatusNotificacao.FALHOU;
        }
    }

    /** Marca a notificação como não-enviável (sem e-mail / cliente inexistente): terminal, sem retentativa. */
    public void marcarNaoEnviavel(LocalDateTime quando) {
        exigirPendente();
        this.ultimaTentativaEm = quando;
        this.status = StatusNotificacao.FALHOU;
    }

    /** Verificação de domínio de elegibilidade (o filtro de carência temporal é aplicado no repositório). */
    public boolean ehReprocessavel(int maxTentativas) {
        return status == StatusNotificacao.PENDENTE && tentativas < maxTentativas;
    }

    private void exigirPendente() {
        if (status != StatusNotificacao.PENDENTE) {
            throw new RegraDeNegocioException("Notificacao so pode ser atualizada enquanto estiver pendente.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public String getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public String getDestinatarioEmail() {
        return destinatarioEmail;
    }

    public String getAssunto() {
        return assunto;
    }

    public String getCorpo() {
        return corpo;
    }

    public String getSituacao() {
        return situacao;
    }

    public StatusNotificacao getStatus() {
        return status;
    }

    public int getTentativas() {
        return tentativas;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getUltimaTentativaEm() {
        return ultimaTentativaEm;
    }
}
