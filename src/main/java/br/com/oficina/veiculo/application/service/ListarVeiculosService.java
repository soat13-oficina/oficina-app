package br.com.oficina.veiculo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.oficina.veiculo.application.query.ListarVeiculosQuery;
import br.com.oficina.veiculo.application.usecase.ListarVeiculosUseCase;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class ListarVeiculosService implements ListarVeiculosUseCase {
    private final VeiculoRepository veiculoRepository;

    public ListarVeiculosService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public List<Veiculo> listarVeiculos(ListarVeiculosQuery query) {
        return veiculoRepository.buscarTodos().stream()
                .filter(veiculo -> query.ano() == null || veiculo.getAno() == query.ano())
                .filter(veiculo -> query.marca() == null || veiculo.getMarca().equalsIgnoreCase(query.marca()))
                .filter(veiculo -> query.fabricante() == null
                        || veiculo.getFabricante().equalsIgnoreCase(query.fabricante()))
                .filter(veiculo -> query.potencia() == null || veiculo.getPotencia() == query.potencia())
                .filter(veiculo -> query.cambio() == null || veiculo.getCambio().equalsIgnoreCase(query.cambio()))
                .filter(veiculo -> query.tipo() == null || veiculo.getTipo() == query.tipo())
                .toList();
    }
}
