package br.com.oficina.pecainsumo.application.usecase;

import java.util.UUID;

/** Baixa do estoque as pecas reservadas para a OS, na conclusao do servico. */
public interface ConsumirReservasDaOrdemUseCase {

    /** @return quantas reservas (pecas distintas) foram consumidas */
    int consumirReservas(UUID ordemDeServicoId);
}
