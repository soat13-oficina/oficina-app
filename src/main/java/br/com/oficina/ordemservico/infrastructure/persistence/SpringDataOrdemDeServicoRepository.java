package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;

public interface SpringDataOrdemDeServicoRepository
        extends JpaRepository<OrdemDeServico, UUID>, JpaSpecificationExecutor<OrdemDeServico> {
    Optional<OrdemDeServico> findByNumeroOrdemServico(String numeroOrdemServico);

    @Query("""
            select o
              from OrdemDeServico o
             where (:numeroOrdemServico is null or lower(o.numeroOrdemServico) = lower(:numeroOrdemServico))
               and (:nomeCliente is null or lower(o.clienteNome) = lower(:nomeCliente))
               and (:placaVeiculo is null or lower(o.veiculoPlaca) = lower(:placaVeiculo))
               and (:documentoCliente is null or o.clienteDocumento = :documentoCliente)
               and o.status not in (
                    br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico.OS_FINALIZADA,
                    br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico.ENTREGUE
               )
             order by
               case
                 when o.status = br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico.SERVICO_EM_ANDAMENTO then 1
                 when o.status = br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico.AGUARDANDO_APROVACAO then 2
                 when o.status in (
                    br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO,
                    br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO
                 ) then 3
                 when o.status = br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico.OS_ABERTA then 4
                 else 5
               end,
               case when o.iniciadaEm is null then 1 else 0 end,
               o.iniciadaEm asc,
               o.numeroOrdemServico asc
            """)
    List<OrdemDeServico> buscarAtivasPriorizadasPorFiltros(
            @Param("numeroOrdemServico") String numeroOrdemServico,
            @Param("nomeCliente") String nomeCliente,
            @Param("placaVeiculo") String placaVeiculo,
            @Param("documentoCliente") String documentoCliente);

    List<OrdemDeServico> findByIniciadaEmIsNotNullAndFinalizadaEmIsNotNull();

    void deleteByNumeroOrdemServico(String numeroOrdemServico);

    boolean existsByFuncionarioId(UUID funcionarioId);
}
