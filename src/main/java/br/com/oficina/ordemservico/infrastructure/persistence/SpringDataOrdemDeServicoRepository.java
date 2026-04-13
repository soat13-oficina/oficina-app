package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.UUID;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface SpringDataOrdemDeServicoRepository extends JpaRepository<OrdemDeServico, UUID> {
    Optional<OrdemDeServico> findByNumeroOrdemServico(String numeroOrdemServico);

    void deleteByNumeroOrdemServico(String numeroOrdemServico);
}
