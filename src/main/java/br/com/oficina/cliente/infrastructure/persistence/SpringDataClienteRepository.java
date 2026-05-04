package br.com.oficina.cliente.infrastructure.persistence;

import java.util.UUID;

import br.com.oficina.cliente.domain.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataClienteRepository extends JpaRepository<Cliente, UUID> {
}
