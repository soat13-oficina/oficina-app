package br.com.oficina.pecainsumo.application.command;

public record AdicionarEstoquePecaCommand(
        String id,
        int quantidade) {
}
