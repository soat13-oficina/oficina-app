package br.com.oficina.ordemservico.saga.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha de auditoria da Saga ({@code saga_passos}): o que foi enviado, recebido, ignorado,
 * compensado ou venceu. E a "historia" exibida em {@code GET /sagas/{ordemServicoId}}.
 */
@Entity
@Table(name = "saga_passos")
public class RegistroPassoSaga {

    private static final int LIMITE_DETALHE = 2000;

    public enum Tipo {
        INICIO,
        ADOCAO,
        ACAO_REGISTRADA,
        COMANDO_ENVIADO,
        /** Mesmo comando, messageId novo: a resposta nao chegou no prazo. */
        COMANDO_REENVIADO,
        EVENTO_RECEBIDO,
        EVENTO_IGNORADO,
        TIMEOUT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassoSaga passo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(name = "mensagem_id")
    private UUID mensagemId;

    @Column(length = LIMITE_DETALHE)
    private String detalhe;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    protected RegistroPassoSaga() {
    }

    public RegistroPassoSaga(UUID sagaId, PassoSaga passo, Tipo tipo, UUID mensagemId, String detalhe, LocalDateTime ocorridoEm) {
        this.sagaId = sagaId;
        this.passo = passo;
        this.tipo = tipo;
        this.mensagemId = mensagemId;
        this.detalhe = detalhe == null ? null : detalhe.substring(0, Math.min(detalhe.length(), LIMITE_DETALHE));
        this.ocorridoEm = ocorridoEm;
    }

    public Long getId() {
        return id;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public PassoSaga getPasso() {
        return passo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public UUID getMensagemId() {
        return mensagemId;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public LocalDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
