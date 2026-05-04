package br.com.oficina.ordemservico.application.command;

public record CriarOrdemDeServicoCommand(String clienteId, String funcionarioId, String placaVeiculo) {
}
