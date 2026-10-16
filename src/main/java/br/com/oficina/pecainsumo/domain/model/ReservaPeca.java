package br.com.oficina.pecainsumo.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quanto de uma peca esta reservado para uma OS (Fase 4). E o que permite liberar "a reserva desta
 * OS" e responder de novo a um {@code ReservarPecas} reenviado sem reservar duas vezes.
 */
public record ReservaPeca(
        UUID ordemDeServicoId,
        String pecaInsumoId,
        int quantidade,
        Status status,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm) {

    public enum Status {
        RESERVADA,
        CONSUMIDA,
        LIBERADA
    }

    public static ReservaPeca nova(UUID ordemDeServicoId, String pecaInsumoId, int quantidade, LocalDateTime agora) {
        return new ReservaPeca(ordemDeServicoId, pecaInsumoId, quantidade, Status.RESERVADA, agora, agora);
    }

    public boolean ativa() {
        return status == Status.RESERVADA;
    }

    public ReservaPeca com(Status novo, LocalDateTime agora) {
        return new ReservaPeca(ordemDeServicoId, pecaInsumoId, quantidade, novo, criadaEm, agora);
    }
}
