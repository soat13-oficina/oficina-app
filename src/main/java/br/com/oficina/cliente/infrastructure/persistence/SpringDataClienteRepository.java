package br.com.oficina.cliente.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataClienteRepository extends JpaRepository<ClienteJpaEntity, String> {
}
