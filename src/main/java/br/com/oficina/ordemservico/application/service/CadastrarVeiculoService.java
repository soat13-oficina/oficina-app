package br.com.oficina.ordemservico.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.usecase.CadastrarVeiculoUseCase;
import br.com.oficina.ordemservico.domain.model.Veiculo;
import br.com.oficina.ordemservico.domain.repository.VeiculoRepository;

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
