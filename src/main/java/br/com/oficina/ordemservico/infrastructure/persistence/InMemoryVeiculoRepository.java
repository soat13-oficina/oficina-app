package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import br.com.oficina.ordemservico.domain.model.Veiculo;
import br.com.oficina.ordemservico.domain.repository.VeiculoRepository;

@Repository
public class InMemoryVeiculoRepository implements VeiculoRepository {
    private final Map<String, Veiculo> veiculos = new ConcurrentHashMap<>();

    @Override
    public void salvar(Veiculo veiculo) {
        veiculos.put(veiculo.getPlaca(), veiculo);
    }

    @Override
    public Optional<Veiculo> buscarPorPlaca(String placa) {
        return Optional.ofNullable(veiculos.get(placa));
    }
}
