package br.com.oficina.veiculo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.oficina.veiculo.application.query.ConsultarVeiculosQuery;
import br.com.oficina.veiculo.application.usecase.ConsultarVeiculosUseCase;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class ConsultarVeiculosService implements ConsultarVeiculosUseCase {
    private final VeiculoRepository veiculoRepository;

    public ConsultarVeiculosService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public List<Veiculo> consultarVeiculos(ConsultarVeiculosQuery query) {
        String placaNormalizada = query.placa() == null ? null : Veiculo.normalizarPlaca(query.placa());
        return veiculoRepository.buscarTodos().stream()
                .filter(veiculo -> placaNormalizada == null || veiculo.getPlaca().equalsIgnoreCase(placaNormalizada))
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
