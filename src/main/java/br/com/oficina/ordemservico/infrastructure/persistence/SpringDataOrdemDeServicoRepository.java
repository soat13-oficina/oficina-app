package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOrdemDeServicoRepository extends JpaRepository<OrdemDeServicoJpaEntity, String> {
    Optional<OrdemDeServicoJpaEntity> findByNumeroOrdemServico(String numeroOrdemServico);

    void deleteByNumeroOrdemServico(String numeroOrdemServico);
}
