package br.com.oficina.ordemservico.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.usecase.CriarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

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
    public void criarOrdemDeServico(CriarOrdemDeServicoCommand command) {
        Cliente cliente = clienteRepository.buscarPorId(command.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado"));
        Veiculo veiculo = veiculoRepository.buscarPorPlaca(command.placaVeiculo())
                .orElseThrow(() -> new IllegalArgumentException("Veiculo nao encontrado"));

        String id = UUID.randomUUID().toString();
        Funcionario funcionario = new Funcionario(command.funcionarioId(), "Funcionario responsavel", null);
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                id,
                "OS-" + id.substring(0, 8).toUpperCase(),
                funcionario,
                cliente,
                veiculo);

        ordemDeServicoRepository.salvar(ordemDeServico);
    }
}
