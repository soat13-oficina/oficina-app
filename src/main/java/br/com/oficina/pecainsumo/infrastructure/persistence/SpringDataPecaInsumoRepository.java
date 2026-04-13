package br.com.oficina.pecainsumo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPecaInsumoRepository extends JpaRepository<PecaInsumoJpaEntity, String> {
}
