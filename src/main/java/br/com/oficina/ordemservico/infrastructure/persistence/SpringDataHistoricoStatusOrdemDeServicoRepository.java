package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataHistoricoStatusOrdemDeServicoRepository
        extends JpaRepository<HistoricoStatusOrdemDeServicoEntity, Long> {

    List<HistoricoStatusOrdemDeServicoEntity> findByOrdemDeServicoIdOrderByOcorridoEmAscIdAsc(UUID ordemDeServicoId);
}
