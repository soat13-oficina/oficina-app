package br.com.oficina.pecainsumo.application.query;

import br.com.oficina.pecainsumo.domain.model.CategoriaPeca;

public record ListarPecasInsumosQuery(
        String marca,
        CategoriaPeca categoria,
        Boolean possuiReserva) {
}
