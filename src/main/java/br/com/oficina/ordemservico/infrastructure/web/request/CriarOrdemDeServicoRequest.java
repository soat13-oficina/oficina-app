package br.com.oficina.ordemservico.infrastructure.web.request;

public record CriarOrdemDeServicoRequest(String clienteId, String funcionarioId, String placaVeiculo) {
}
