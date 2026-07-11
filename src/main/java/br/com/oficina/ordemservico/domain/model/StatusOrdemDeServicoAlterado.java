package br.com.oficina.ordemservico.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatusOrdemDeServicoAlterado(
        String numeroOrdemServico,
        UUID clienteId,
        SituacaoOrdemDeServico situacaoAnterior,
        SituacaoOrdemDeServico novaSituacao,
        LocalDateTime ocorridoEm) {
}
