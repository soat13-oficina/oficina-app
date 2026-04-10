package br.com.oficina.ordemservico.infrastructure.controller.dto;

public record CriarOrdemDeServicoRequest(String clienteId, String funcionarioId, String placaVeiculo) {
}
