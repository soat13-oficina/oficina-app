package br.com.oficina.ordemservico.application.command;

public record AlterarOrdemDeServicoCommand(
        String numeroOrdemServico,
        String clienteId,
        String funcionarioId,
        String placaVeiculo) {
}
