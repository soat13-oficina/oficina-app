package br.com.oficina.pecainsumo.application.command;

public record LiberarReservaPecaCommand(
        String id,
        int quantidade) {
}
