package br.com.oficina.orcamento.application.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.CadastrarNovoOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class CadastrarNovoOrcamentoService implements CadastrarNovoOrcamentoUseCase {
    private static final Logger log = LoggerFactory.getLogger(CadastrarNovoOrcamentoService.class);

    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;

    public CadastrarNovoOrcamentoService(OrcamentoRepository orcamentoRepository, ClienteRepository clienteRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void cadastrarNovoOrcamento(CadastrarNovoOrcamentoCommand command) {
        log.info("Iniciando cadastro de orcamento. numeroOrcamento={}, clienteId={}, ordemDeServicoId={}",
                command.numeroOrcamento(), command.clienteId(), command.ordemDeServicoId());
        if (orcamentoRepository.buscarPorNumeroOrcamento(command.numeroOrcamento()).isPresent()) {
            throw new RegraDeNegocioException("Ja existe orcamento cadastrado com o mesmo numero.");
        }
        UUID clienteId = paraUuid(command.clienteId(), "Identificador do cliente invalido.");
        UUID ordemDeServicoId = paraUuid(command.ordemDeServicoId(), "Identificador da ordem de servico invalido.");
        UUID funcionarioId = paraUuid(command.funcionarioId(), "Identificador do funcionario invalido.");
        Cliente cliente = clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));
        Orcamento orcamento = new Orcamento(
                command.numeroOrcamento(),
                cliente.getId(),
                ordemDeServicoId,
                funcionarioId,
                cliente.getNome(),
                cliente.getCpfOuCnpj(),
                command.placaVeiculo(),
                command.marcaVeiculo(),
                command.modeloVeiculo(),
                command.descricaoDiagnostico(),
                command.servicosPropostos(),
                command.pecasPrevistas(),
                command.valorMaoDeObra(),
                command.desconto(),
                LocalDateTime.now(),
                command.validade(),
                command.observacoes(),
                StatusOrcamento.AGUARDANDO_APROVACAO);

        orcamentoRepository.salvar(orcamento);
        log.info("Orcamento cadastrado com sucesso. numeroOrcamento={}", command.numeroOrcamento());
    }

    private UUID paraUuid(String valor, String mensagemErro) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException exception) {
            throw new RegraDeNegocioException(mensagemErro);
        }
    }
}
