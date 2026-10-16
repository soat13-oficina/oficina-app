package br.com.oficina.pecainsumo.domain.repository;

import java.util.List;
import java.util.UUID;

import br.com.oficina.pecainsumo.domain.model.ReservaPeca;

public interface ReservaPecaRepository {

    /** Grava ou substitui a reserva da peca para a OS (chave: OS + peca). */
    void salvar(ReservaPeca reserva);

    List<ReservaPeca> buscarPorOrdemDeServico(UUID ordemDeServicoId);
}
