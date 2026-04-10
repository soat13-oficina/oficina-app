package br.com.oficina.ordemservico.domain.repository;

import java.util.Optional;

import br.com.oficina.ordemservico.domain.model.Veiculo;

public interface VeiculoRepository {
    void salvar(Veiculo veiculo);

    Optional<Veiculo> buscarPorPlaca(String placa);
}
