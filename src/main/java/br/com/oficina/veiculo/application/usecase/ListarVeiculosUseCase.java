package br.com.oficina.veiculo.application.usecase;

import java.util.List;

import br.com.oficina.veiculo.application.query.ListarVeiculosQuery;
import br.com.oficina.veiculo.domain.model.Veiculo;

public interface ListarVeiculosUseCase {
    List<Veiculo> listarVeiculos(ListarVeiculosQuery query);
}
