package br.com.oficina.ordemservico.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.CriarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.usecase.CriarNovaOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class CriarNovaOrdemDeServicoService implements CriarNovaOrdemDeServicoUseCase {
    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public CriarNovaOrdemDeServicoService(
            ClienteRepository clienteRepository,
            VeiculoRepository veiculoRepository,
            FuncionarioRepository funcionarioRepository,
            OrdemDeServicoRepository ordemDeServicoRepository) {
        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.ordemDeServicoRepository = ordemDeServicoRepository;
    }

    @Override
    public void criarNovaOrdemDeServico(CriarOrdemDeServicoCommand command) {
        Cliente cliente = clienteRepository.buscarPorId(paraUuid(command.clienteId(), "Identificador do cliente invalido."))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));
        Veiculo veiculo = veiculoRepository.buscarPorPlaca(command.placaVeiculo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado para a placa informada."));
        Funcionario funcionario = funcionarioRepository
                .buscarPorId(paraUuid(command.funcionarioId(), "Identificador do funcionario invalido."))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario nao encontrado para o identificador informado."));
        if (!veiculo.getClienteId().equals(cliente.getId())) {
            throw new RegraDeNegocioException("Veiculo informado nao pertence ao cliente selecionado.");
        }

        UUID identificador = UUID.randomUUID();
        OrdemDeServico ordemDeServico = OrdemDeServico.abrir(
                null,
                "OS-" + identificador.toString().substring(0, 8).toUpperCase(),
                funcionario,
                cliente,
                veiculo);

        ordemDeServicoRepository.salvar(ordemDeServico);
    }

    private UUID paraUuid(String valor, String mensagemErro) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException exception) {
            throw new RegraDeNegocioException(mensagemErro);
        }
    }
}
