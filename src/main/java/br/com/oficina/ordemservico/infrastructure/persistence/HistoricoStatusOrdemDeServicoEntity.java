package br.com.oficina.ordemservico.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import br.com.oficina.ordemservico.domain.model.AlteracaoStatusOrdemDeServico;
import br.com.oficina.ordemservico.domain.model.OrigemAlteracaoStatus;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha da tabela {@code ordem_servico_historico} (migration V21). So escrita (append-only):
 * o historico nunca e alterado, apenas acrescido e apagado junto com a ordem (ON DELETE CASCADE).
 */
@Entity
@Table(name = "ordem_servico_historico")
public class HistoricoStatusOrdemDeServicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ordem_de_servico_id", nullable = false)
    private UUID ordemDeServicoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior")
    private StatusOrdemDeServico statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false)
    private StatusOrdemDeServico statusNovo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemAlteracaoStatus origem;

    @Column(name = "saga_id")
    private UUID sagaId;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    protected HistoricoStatusOrdemDeServicoEntity() {
    }

    static HistoricoStatusOrdemDeServicoEntity de(UUID ordemDeServicoId, AlteracaoStatusOrdemDeServico alteracao) {
        HistoricoStatusOrdemDeServicoEntity entity = new HistoricoStatusOrdemDeServicoEntity();
        entity.ordemDeServicoId = ordemDeServicoId;
        entity.statusAnterior = alteracao.statusAnterior();
        entity.statusNovo = alteracao.statusNovo();
        entity.origem = alteracao.origem();
        entity.sagaId = alteracao.sagaId();
        entity.ocorridoEm = alteracao.ocorridoEm();
        return entity;
    }

    AlteracaoStatusOrdemDeServico paraDominio() {
        return new AlteracaoStatusOrdemDeServico(statusAnterior, statusNovo, origem, sagaId, ocorridoEm);
    }
}
