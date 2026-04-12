package br.com.oficina.veiculo.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVeiculoRepository extends JpaRepository<VeiculoJpaEntity, String> {
}
