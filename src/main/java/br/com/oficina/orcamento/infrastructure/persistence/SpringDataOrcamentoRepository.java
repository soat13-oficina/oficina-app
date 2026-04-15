package br.com.oficina.orcamento.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import br.com.oficina.orcamento.domain.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOrcamentoRepository extends JpaRepository<Orcamento, UUID> {
    Optional<Orcamento> findByNumeroOrcamento(String numeroOrcamento);

    Optional<Orcamento> findByOrdemDeServicoId(String ordemDeServicoId);

    void deleteByNumeroOrcamento(String numeroOrcamento);
}
