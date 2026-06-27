package br.com.oficina.notificacao.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataNotificacaoRepository extends JpaRepository<NotificacaoJpaEntity, UUID> {

    @Query("""
            SELECT n FROM NotificacaoJpaEntity n
            WHERE n.status = br.com.oficina.notificacao.domain.model.StatusNotificacao.PENDENTE
              AND n.tentativas < :maxTentativas
              AND COALESCE(n.ultimaTentativaEm, n.criadoEm) < :anteriorA
            """)
    List<NotificacaoJpaEntity> buscarReprocessaveis(
            @Param("maxTentativas") int maxTentativas,
            @Param("anteriorA") LocalDateTime anteriorA);
}
