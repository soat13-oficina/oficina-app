package br.com.oficina.veiculo.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import br.com.oficina.veiculo.domain.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpringDataVeiculoRepository extends JpaRepository<Veiculo, UUID>, JpaSpecificationExecutor<Veiculo> {
    Optional<Veiculo> findByPlaca(String placa);

    void deleteByPlaca(String placa);
}
