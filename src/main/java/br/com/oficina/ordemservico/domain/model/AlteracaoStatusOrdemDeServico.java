package br.com.oficina.ordemservico.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma mudanca de status da ordem de servico, como aparece no historico.
 *
 * @param statusAnterior {@code null} na abertura da OS (e nas linhas de {@link OrigemAlteracaoStatus#MIGRACAO})
 * @param sagaId         Saga que provocou a mudanca, quando a origem e {@link OrigemAlteracaoStatus#SAGA}
 *                       ou {@link OrigemAlteracaoStatus#TIMEOUT}
 */
public record AlteracaoStatusOrdemDeServico(
        StatusOrdemDeServico statusAnterior,
        StatusOrdemDeServico statusNovo,
        OrigemAlteracaoStatus origem,
        UUID sagaId,
        LocalDateTime ocorridoEm) {

    public SituacaoOrdemDeServico situacaoNova() {
        return SituacaoOrdemDeServico.fromStatus(statusNovo);
    }
}
