package br.com.oficina.notificacao.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.oficina.notificacao.domain.model.Notificacao;
import br.com.oficina.notificacao.domain.model.StatusNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notificacoes")
public class NotificacaoJpaEntity {
    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "numero_ordem_servico", nullable = false)
    private String numeroOrdemServico;

    @Column(name = "destinatario_email")
    private String destinatarioEmail;

    @Column(nullable = false)
    private String assunto;

    @Column(nullable = false, length = 2000)
    private String corpo;

    @Column(nullable = false)
    private String situacao;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusNotificacao status;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "ultima_tentativa_em")
    private LocalDateTime ultimaTentativaEm;

    protected NotificacaoJpaEntity() {
    }

    private NotificacaoJpaEntity(
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

    public static NotificacaoJpaEntity fromDomain(Notificacao notificacao) {
        return new NotificacaoJpaEntity(
                notificacao.getId(),
                notificacao.getClienteId(),
                notificacao.getNumeroOrdemServico(),
                notificacao.getDestinatarioEmail(),
                notificacao.getAssunto(),
                notificacao.getCorpo(),
                notificacao.getSituacao(),
                notificacao.getStatus(),
                notificacao.getTentativas(),
                notificacao.getCriadoEm(),
                notificacao.getUltimaTentativaEm());
    }

    public Notificacao toDomain() {
        return Notificacao.reconstituir(
                id,
                clienteId,
                numeroOrdemServico,
                destinatarioEmail,
                assunto,
                corpo,
                situacao,
                status,
                tentativas,
                criadoEm,
                ultimaTentativaEm);
    }
}
