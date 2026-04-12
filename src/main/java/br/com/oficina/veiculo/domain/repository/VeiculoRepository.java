package br.com.oficina.veiculo.domain.repository;

import java.util.List;
import java.util.Optional;

import br.com.oficina.veiculo.domain.model.Veiculo;

public interface VeiculoRepository {
    void salvar(Veiculo veiculo);

    Optional<Veiculo> buscarPorPlaca(String placa);

    List<Veiculo> buscarTodos();

    void excluirPorPlaca(String placa);
}
