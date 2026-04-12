package br.com.oficina.pecainsumo.application.query;

public record ListarPecasInsumosQuery(
        String marca,
        String categoria,
        Boolean possuiReserva) {
}
