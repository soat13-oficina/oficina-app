package br.com.oficina.orcamento.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOrcamentoRepository extends JpaRepository<OrcamentoJpaEntity, String> {
}
