package br.com.oficina.ordemservico.infrastructure.web.request;

public record AlterarOrdemDeServicoRequest(
        String clienteId,
        String funcionarioId,
        String placaVeiculo) {
}
