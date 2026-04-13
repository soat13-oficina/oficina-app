package br.com.oficina.veiculo.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.application.command.CadastrarVeiculoCommand;
import br.com.oficina.veiculo.application.usecase.CadastrarVeiculoUseCase;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class CadastrarVeiculoService implements CadastrarVeiculoUseCase {
    private static final Logger log = LoggerFactory.getLogger(CadastrarVeiculoService.class);

    private final VeiculoRepository veiculoRepository;

    public CadastrarVeiculoService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public void cadastrarVeiculo(CadastrarVeiculoCommand command) {
        log.info("Iniciando cadastro de veiculo. placaInformada={}, marca={}, fabricante={}",
                command.placa(),
                command.marca(),
                command.fabricante());
        if (veiculoRepository.buscarPorPlaca(command.placa()).isPresent()) {
            throw new RegraDeNegocioException("Ja existe veiculo cadastrado com a mesma placa.");
        }

        Veiculo veiculo = new Veiculo(
                command.placa(),
                command.marca(),
                command.modelo(),
                command.fabricante(),
                command.ano(),
                command.potencia(),
                command.cambio(),
                command.tipo());
        veiculoRepository.salvar(veiculo);
        log.info("Veiculo cadastrado com sucesso. veiculoId={}, placa={}", veiculo.getId(), veiculo.getPlaca());
    }
}
