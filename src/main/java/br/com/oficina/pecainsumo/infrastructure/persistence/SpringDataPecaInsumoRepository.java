package br.com.oficina.pecainsumo.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SpringDataPecaInsumoRepository extends JpaRepository<PecaInsumoJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PecaInsumoJpaEntity p where p.id = :id")
    Optional<PecaInsumoJpaEntity> buscarComTrava(@Param("id") String id);
}
