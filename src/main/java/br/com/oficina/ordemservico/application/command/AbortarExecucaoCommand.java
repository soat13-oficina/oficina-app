package br.com.oficina.ordemservico.application.command;

public record AbortarExecucaoCommand(String numeroOrdemServico, String motivo) {
}
