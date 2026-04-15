package br.com.oficina.veiculo.application.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.veiculo.application.query.ConsultarVeiculosQuery;
import br.com.oficina.veiculo.application.usecase.ConsultarVeiculosUseCase;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class ConsultarVeiculosService implements ConsultarVeiculosUseCase {
    private static final Logger log = LoggerFactory.getLogger(ConsultarVeiculosService.class);

    private final VeiculoRepository veiculoRepository;

    public ConsultarVeiculosService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public List<Veiculo> consultarVeiculos(ConsultarVeiculosQuery query) {
        log.info("Consultando veiculos. placaInformada={}, marca={}, fabricante={}, ano={}, tipo={}",
                query.placa(),
                query.marca(),
                query.fabricante(),
                query.ano(),
                query.tipo());
        List<Veiculo> veiculos = veiculoRepository.buscarPorFiltros(
                query.placa(),
                query.ano(),
                query.marca(),
                query.fabricante(),
                query.potencia(),
                query.cambio(),
                query.tipo());
        log.info("Consulta de veiculos finalizada. quantidadeVeiculos={}", veiculos.size());
        return veiculos;
    }
}
