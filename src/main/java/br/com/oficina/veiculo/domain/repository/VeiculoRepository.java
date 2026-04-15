package br.com.oficina.veiculo.domain.repository;

import java.util.List;
import java.util.Optional;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;

public interface VeiculoRepository {
    void salvar(Veiculo veiculo);

    Optional<Veiculo> buscarPorPlaca(String placa);

    List<Veiculo> buscarPorFiltros(
            String placa,
            Integer ano,
            String marca,
            String fabricante,
            Integer potencia,
            String cambio,
            TipoCombustivel tipo);

    List<Veiculo> buscarTodos();

    void excluirPorPlaca(String placa);
}
