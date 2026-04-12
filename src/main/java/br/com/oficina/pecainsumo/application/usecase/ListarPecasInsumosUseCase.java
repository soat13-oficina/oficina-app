package br.com.oficina.pecainsumo.application.usecase;

import java.util.List;

import br.com.oficina.pecainsumo.application.query.ListarPecasInsumosQuery;
import br.com.oficina.pecainsumo.domain.model.PecaInsumo;

public interface ListarPecasInsumosUseCase {
    List<PecaInsumo> listarPecasInsumos(ListarPecasInsumosQuery query);
}
