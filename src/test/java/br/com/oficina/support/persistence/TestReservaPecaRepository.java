package br.com.oficina.support.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.pecainsumo.domain.model.ReservaPeca;
import br.com.oficina.pecainsumo.domain.repository.ReservaPecaRepository;

public class TestReservaPecaRepository implements ReservaPecaRepository {

    private record Chave(UUID ordemDeServicoId, String pecaInsumoId) {
    }

    private final Map<Chave, ReservaPeca> reservas = new ConcurrentHashMap<>();

    @Override
    public void salvar(ReservaPeca reserva) {
        reservas.put(new Chave(reserva.ordemDeServicoId(), reserva.pecaInsumoId()), reserva);
    }

    @Override
    public List<ReservaPeca> buscarPorOrdemDeServico(UUID ordemDeServicoId) {
        return reservas.values().stream()
                .filter(r -> r.ordemDeServicoId().equals(ordemDeServicoId))
                .sorted(Comparator.comparing(ReservaPeca::pecaInsumoId))
                .toList();
    }
}
