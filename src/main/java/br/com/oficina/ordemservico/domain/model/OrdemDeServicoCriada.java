package br.com.oficina.ordemservico.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abertura de uma ordem de servico.
 *
 * <p>Existe separado de {@link StatusOrdemDeServicoAlterado} porque abrir a OS nao e uma
 * transicao: OS_ABERTA e o estado inicial, nao o destino de uma mudanca. Contar
 * {@code RECEBIDA -> DIAGNOSTICO} como proxy de "OS criada" subcontaria toda ordem que nunca
 * chegou a virar diagnostico, e reaproveitar o evento de transicao dispararia notificacao ao
 * cliente na criacao - comportamento que a aplicacao nao tem hoje.
 *
 * <p>Consumido apenas por observabilidade ({@code MetricasOrdemServicoListener}), que o traduz
 * no contador {@code oficina.os.criadas} - a fonte do "volume diario de OS" no dashboard.
 */
public record OrdemDeServicoCriada(
        String numeroOrdemServico,
        UUID clienteId,
        UUID funcionarioId,
        LocalDateTime criadaEm) {

    public static OrdemDeServicoCriada de(OrdemDeServico ordemDeServico) {
        return new OrdemDeServicoCriada(
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getCliente().getId(),
                ordemDeServico.getFuncionario().getId(),
                ordemDeServico.getSituacaoAlteradaEm());
    }
}
