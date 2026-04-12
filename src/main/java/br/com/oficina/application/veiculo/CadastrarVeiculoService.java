package br.com.oficina.application.veiculo;

import org.springframework.stereotype.Service;

import br.com.oficina.application.veiculo.CadastrarVeiculoUseCase;
import br.com.oficina.domain.model.veiculo.Veiculo;
import br.com.oficina.domain.repository.VeiculoRepository;

@Service
public class CadastrarVeiculoService implements CadastrarVeiculoUseCase {
    private final VeiculoRepository veiculoRepository;

    public CadastrarVeiculoService(VeiculoRepository veiculoRepository) {
        this.veiculoRepository = veiculoRepository;
    }

    @Override
    public void cadastrarVeiculo(CadastrarVeiculoRequest request) {
        veiculoRepository.salvar(new Veiculo(request.placa(), request.marca(), request.modelo()));
    }
}
