package br.com.oficina.ordemservico.saga.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.oficina.ordemservico.saga.domain.model.RegistroPassoSaga;

public interface SpringDataRegistroPassoSagaRepository extends JpaRepository<RegistroPassoSaga, Long> {

    List<RegistroPassoSaga> findBySagaIdOrderByOcorridoEmAscIdAsc(UUID sagaId);
}
