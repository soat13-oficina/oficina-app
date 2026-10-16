package br.com.oficina.pecainsumo.infrastructure.persistence;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import br.com.oficina.pecainsumo.domain.model.ReservaPeca;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservas_pecas")
@IdClass(ReservaPecaJpaEntity.Chave.class)
public class ReservaPecaJpaEntity {

    @Id
    @Column(name = "ordem_de_servico_id", nullable = false)
    private UUID ordemDeServicoId;

    @Id
    @Column(name = "peca_insumo_id", nullable = false)
    private String pecaInsumoId;

    @Column(nullable = false)
    private int quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservaPeca.Status status;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected ReservaPecaJpaEntity() {
    }

    static ReservaPecaJpaEntity fromDomain(ReservaPeca reserva) {
        ReservaPecaJpaEntity entity = new ReservaPecaJpaEntity();
        entity.ordemDeServicoId = reserva.ordemDeServicoId();
        entity.pecaInsumoId = reserva.pecaInsumoId();
        entity.quantidade = reserva.quantidade();
        entity.status = reserva.status();
        entity.criadaEm = reserva.criadaEm();
        entity.atualizadaEm = reserva.atualizadaEm();
        return entity;
    }

    ReservaPeca toDomain() {
        return new ReservaPeca(ordemDeServicoId, pecaInsumoId, quantidade, status, criadaEm, atualizadaEm);
    }

    public static class Chave implements Serializable {
        private UUID ordemDeServicoId;
        private String pecaInsumoId;

        public Chave() {
        }

        public Chave(UUID ordemDeServicoId, String pecaInsumoId) {
            this.ordemDeServicoId = ordemDeServicoId;
            this.pecaInsumoId = pecaInsumoId;
        }

        @Override
        public boolean equals(Object outro) {
            return outro instanceof Chave chave
                    && Objects.equals(ordemDeServicoId, chave.ordemDeServicoId)
                    && Objects.equals(pecaInsumoId, chave.pecaInsumoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ordemDeServicoId, pecaInsumoId);
        }
    }
}
