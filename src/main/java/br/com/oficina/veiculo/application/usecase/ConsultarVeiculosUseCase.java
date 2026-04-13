package br.com.oficina.veiculo.application.usecase;

import java.util.List;

import br.com.oficina.veiculo.application.query.ConsultarVeiculosQuery;
import br.com.oficina.veiculo.domain.model.Veiculo;

public interface ConsultarVeiculosUseCase {
    List<Veiculo> consultarVeiculos(ConsultarVeiculosQuery query);
}
