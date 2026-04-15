package br.com.oficina.support.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

public class TestVeiculoRepository implements VeiculoRepository {
    private final Map<String, Veiculo> veiculos = new ConcurrentHashMap<>();

    @Override
    public void salvar(Veiculo veiculo) {
        Veiculo veiculoPersistido = veiculo.getId() == null
                ? Veiculo.reconstituir(
                        java.util.UUID.randomUUID(),
                        veiculo.getClienteId(),
                        veiculo.getPlaca(),
                        veiculo.getMarca(),
                        veiculo.getModelo(),
                        veiculo.getFabricante(),
                        veiculo.getAno(),
                        veiculo.getPotencia(),
                        veiculo.getCambio(),
                        veiculo.getTipo())
                : veiculo;
        veiculos.put(veiculoPersistido.getPlaca(), veiculoPersistido);
    }

    @Override
    public Optional<Veiculo> buscarPorPlaca(String placa) {
        return Optional.ofNullable(veiculos.get(Veiculo.normalizarPlaca(placa)));
    }

    @Override
    public List<Veiculo> buscarPorFiltros(
            String placa,
            Integer ano,
            String marca,
            String fabricante,
            Integer potencia,
            String cambio,
            TipoCombustivel tipo) {
        String placaNormalizada = placa == null ? null : Veiculo.normalizarPlaca(placa);
        return veiculos.values().stream()
                .filter(veiculo -> placaNormalizada == null || veiculo.getPlaca().equalsIgnoreCase(placaNormalizada))
                .filter(veiculo -> ano == null || veiculo.getAno() == ano)
                .filter(veiculo -> marca == null || veiculo.getMarca().equalsIgnoreCase(marca))
                .filter(veiculo -> fabricante == null || veiculo.getFabricante().equalsIgnoreCase(fabricante))
                .filter(veiculo -> potencia == null || veiculo.getPotencia() == potencia)
                .filter(veiculo -> cambio == null || veiculo.getCambio().equalsIgnoreCase(cambio))
                .filter(veiculo -> tipo == null || veiculo.getTipo() == tipo)
                .toList();
    }

    @Override
    public List<Veiculo> buscarTodos() {
        return List.copyOf(veiculos.values());
    }

    @Override
    public void excluirPorPlaca(String placa) {
        veiculos.remove(Veiculo.normalizarPlaca(placa));
    }
}
