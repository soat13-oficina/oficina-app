package br.com.oficina.pecainsumo.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.oficina.pecainsumo.domain.model.ReservaPeca;
import br.com.oficina.pecainsumo.domain.repository.ReservaPecaRepository;

@Repository
@Transactional(readOnly = true)
public class JpaReservaPecaRepository implements ReservaPecaRepository {

    private final SpringDataReservaPecaRepository repository;

    public JpaReservaPecaRepository(SpringDataReservaPecaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvar(ReservaPeca reserva) {
        repository.save(ReservaPecaJpaEntity.fromDomain(reserva));
    }

    @Override
    public List<ReservaPeca> buscarPorOrdemDeServico(UUID ordemDeServicoId) {
        return repository.findByOrdemDeServicoIdOrderByPecaInsumoId(ordemDeServicoId).stream()
                .map(ReservaPecaJpaEntity::toDomain)
                .toList();
    }
}
