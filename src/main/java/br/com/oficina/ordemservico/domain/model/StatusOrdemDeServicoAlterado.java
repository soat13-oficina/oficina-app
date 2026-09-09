package br.com.oficina.ordemservico.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transicao de SITUACAO de negocio de uma ordem de servico.
 *
 * <p>Ponto unico de instrumentacao do ciclo de vida da OS: notificacao ao cliente
 * ({@code EnviarNotificacaoStatusOSService}) e metricas ({@code MetricasOrdemServicoListener})
 * penduram-se aqui, sem que nenhum caso de uso precise conhecer observabilidade.
 *
 * <p>{@code anteriorDesde} e o instante em que a ordem ENTROU em {@code situacaoAnterior};
 * {@code ocorridoEm - anteriorDesde} e portanto quanto tempo ela permaneceu naquela etapa - a
 * origem do "tempo medio de execucao por status". Pode vir {@code null} em ordens anteriores a
 * migration V19 cujo backfill nao tinha timestamp para usar.
 */
public record StatusOrdemDeServicoAlterado(
        String numeroOrdemServico,
        UUID clienteId,
        SituacaoOrdemDeServico situacaoAnterior,
        SituacaoOrdemDeServico novaSituacao,
        LocalDateTime anteriorDesde,
        LocalDateTime ocorridoEm) {

    /**
     * Monta o evento a partir da ordem JA transicionada. {@code ocorridoEm} vem de
     * {@code situacaoAlteradaEm} (e nao de {@code now()}) para que o evento carregue exatamente o
     * mesmo instante gravado no banco - sem isso, duracao medida e duracao persistida divergem.
     *
     * @param anteriorDesde valor de {@code getSituacaoAlteradaEm()} lido ANTES da transicao
     */
    public static StatusOrdemDeServicoAlterado de(
            OrdemDeServico ordemDeServico,
            SituacaoOrdemDeServico situacaoAnterior,
            LocalDateTime anteriorDesde) {
        return new StatusOrdemDeServicoAlterado(
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getCliente().getId(),
                situacaoAnterior,
                ordemDeServico.getSituacao(),
                anteriorDesde,
                ordemDeServico.getSituacaoAlteradaEm());
    }

    /** Tempo que a ordem permaneceu em {@link #situacaoAnterior()}, quando mensuravel. */
    public Duration duracaoNaSituacaoAnterior() {
        if (anteriorDesde == null || ocorridoEm == null || ocorridoEm.isBefore(anteriorDesde)) {
            return null;
        }
        return Duration.between(anteriorDesde, ocorridoEm);
    }
}
