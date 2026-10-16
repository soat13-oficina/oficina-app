package br.com.oficina.pecainsumo.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataReservaPecaRepository extends JpaRepository<ReservaPecaJpaEntity, ReservaPecaJpaEntity.Chave> {

    List<ReservaPecaJpaEntity> findByOrdemDeServicoIdOrderByPecaInsumoId(UUID ordemDeServicoId);
}
