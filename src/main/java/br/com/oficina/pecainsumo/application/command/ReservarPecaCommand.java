package br.com.oficina.pecainsumo.application.command;

public record ReservarPecaCommand(
        String id,
        int quantidade) {
}
