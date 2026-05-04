package br.com.oficina.ordemservico.application.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.application.command.AlterarOrdemDeServicoCommand;
import br.com.oficina.ordemservico.application.usecase.AlterarOrdemDeServicoUseCase;
import br.com.oficina.ordemservico.domain.repository.FuncionarioRepository;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.repository.OrdemDeServicoRepository;
import br.com.oficina.veiculo.domain.model.Veiculo;
import br.com.oficina.veiculo.domain.repository.VeiculoRepository;

@Service
public class AlterarOrdemDeServicoService implements AlterarOrdemDeServicoUseCase {
    private static final Logger log = LoggerFactory.getLogger(AlterarOrdemDeServicoService.class);

    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final OrdemDeServicoRepository ordemDeServicoRepository;

    public AlterarOrdemDeServicoService(
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
    public void alterarOrdemDeServico(AlterarOrdemDeServicoCommand command) {
        log.info(
                "Iniciando alteracao de ordem de servico. numeroOrdemServico={}, clienteId={}, funcionarioId={}, placaVeiculo={}",
                command.numeroOrdemServico(),
                command.clienteId(),
                command.funcionarioId(),
                command.placaVeiculo());
        UUID funcionarioId = paraUuid(command.funcionarioId(), "Identificador do funcionario invalido.");
        OrdemDeServico ordemDeServico = ordemDeServicoRepository.buscarPorNumero(command.numeroOrdemServico())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de servico nao encontrada para o numero informado."));
        Cliente cliente = clienteRepository.buscarPorId(paraUuid(command.clienteId(), "Identificador do cliente invalido."))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));
        Veiculo veiculo = veiculoRepository.buscarPorPlaca(command.placaVeiculo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veiculo nao encontrado para a placa informada."));
        funcionarioRepository.buscarPorId(funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario nao encontrado para o identificador informado."));
        if (!veiculo.getClienteId().equals(cliente.getId())) {
            throw new RegraDeNegocioException("Veiculo informado nao pertence ao cliente selecionado.");
        }
        if (!ordemDeServico.getFuncionario().getId().equals(funcionarioId)) {
            throw new RegraDeNegocioException("Funcionario criador da ordem de servico nao pode ser alterado.");
        }

        ordemDeServico.alterar(cliente, veiculo);
        ordemDeServicoRepository.salvar(ordemDeServico);
        log.info(
                "Ordem de servico alterada com sucesso. numeroOrdemServico={}, clienteId={}, funcionarioId={}, placaVeiculo={}",
                ordemDeServico.getNumeroOrdemServico(),
                cliente.getId(),
                funcionarioId,
                veiculo.getPlaca());
    }

    private UUID paraUuid(String valor, String mensagemErro) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException exception) {
            throw new RegraDeNegocioException(mensagemErro);
        }
    }
}
