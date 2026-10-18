package br.com.oficina.ordemservico.saga.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.oficina.ordemservico.saga.domain.model.SagaOrdemServico;
import br.com.oficina.ordemservico.saga.domain.model.StatusSaga;

public interface SpringDataSagaOrdemServicoRepository extends JpaRepository<SagaOrdemServico, UUID> {

    Optional<SagaOrdemServico> findByOrdemDeServicoId(UUID ordemDeServicoId);

    @Query("""
            select s.id
              from SagaOrdemServico s
             where s.prazoExpiraEm < :agora
               and s.status not in :finais
             order by s.prazoExpiraEm
            """)
    List<UUID> buscarIdsComPrazoVencido(LocalDateTime agora, Collection<StatusSaga> finais);

    /** Funil da Saga: [passo, quantidade] das nao finalizadas (metrica oficina.saga.ativas). */
    @Query("""
            select s.passoAtual, count(s)
              from SagaOrdemServico s
             where s.status not in :finais
             group by s.passoAtual
            """)
    List<Object[]> contarAtivasPorPasso(Collection<StatusSaga> finais);
}
