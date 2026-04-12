package br.com.oficina.application.ordemservico;

public record CriarOrdemDeServicoRequest(String clienteId, String funcionarioId, String placaVeiculo) {
}
