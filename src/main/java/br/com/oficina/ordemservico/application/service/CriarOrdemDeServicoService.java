package br.com.oficina.ordemservico.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.oficina.ordemservico.application.usecase.CriarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.Cliente;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.Veiculo;
import br.com.oficina.ordemservico.domain.repository.ClienteRepository;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.ordemservico.domain.repository.VeiculoRepository;

@Service
public class CriarOrdemDeServicoService implements CriarOrdemDeServicoUseCase {
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public CriarOrdemDeServicoService(
            ClienteRepository clienteRepository,
            VeiculoRepository veiculoRepository,
            OrdemDeServicoRepository ordemDeServicoRepository) {
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void criarOrdemDeServico(CriarOrdemDeServicoRequest request) {
        Cliente cliente = clienteRepository.buscarPorId(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado"));
        Veiculo veiculo = veiculoRepository.buscarPorPlaca(request.placaVeiculo())
                .orElseThrow(() -> new IllegalArgumentException("Veiculo nao encontrado"));

        Funcionario funcionario = new Funcionario(request.funcionarioId(), "Funcionario responsavel", null);
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                UUID.randomUUID().toString(),
                funcionario,
                cliente,
                veiculo);

        ordemDeServicoRepository.salvar(ordemDeServico);
    }
}
