package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface SpringDataOrdemDeServicoRepository
        extends JpaRepository<OrdemDeServico, UUID>, JpaSpecificationExecutor<OrdemDeServico> {
    Optional<OrdemDeServico> findByNumeroOrdemServico(String numeroOrdemServico);

    List<OrdemDeServico> findByIniciadaEmIsNotNullAndFinalizadaEmIsNotNull();

    void deleteByNumeroOrdemServico(String numeroOrdemServico);

    boolean existsByFuncionarioId(UUID funcionarioId);
}
