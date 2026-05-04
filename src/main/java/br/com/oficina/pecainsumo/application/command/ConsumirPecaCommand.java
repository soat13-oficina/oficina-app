package br.com.oficina.pecainsumo.application.command;

public record ConsumirPecaCommand(
        String id,
        int quantidade) {
}
