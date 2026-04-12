package br.com.oficina.pecainsumo.application.command;

public record RemoverEstoquePecaCommand(
        String id,
        int quantidade) {
}
