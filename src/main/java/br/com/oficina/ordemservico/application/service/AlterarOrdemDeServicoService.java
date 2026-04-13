package br.com.oficina.ordemservico.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.AlterarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.usecase.AlterarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class AlterarOrdemDeServicoService implements AlterarOrdemDeServicoUseCase {
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public AlterarOrdemDeServicoService(
            ClienteRepository clienteRepository,
            VeiculoRepository veiculoRepository,
            OrdemDeServicoRepository ordemDeServicoRepository) {
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void alterarOrdemDeServico(AlterarOrdemDeServicoCommand command) {
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new IllegalArgumentException("Ordem de servico nao encontrada"));
        Cliente cliente = clienteRepository.buscarPorId(UUID.fromString(command.clienteId()))
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado"));
        Veiculo veiculo = veiculoRepository.buscarPorPlaca(command.placaVeiculo())
                .orElseThrow(() -> new IllegalArgumentException("Veiculo nao encontrado"));
        if (!veiculo.getClienteId().equals(cliente.getId())) {
            throw new RegraDeNegocioException("Veiculo informado nao pertence ao cliente selecionado.");
        }
        Funcionario funcionario = new Funcionario(command.funcionarioId(), "Funcionario responsavel", null);

        ordemDeServico.alterar(funcionario, cliente, veiculo);
        ordemDeServicoRepository.salvar(ordemDeServico);
    }
}
