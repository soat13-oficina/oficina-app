package br.com.oficina.orcamento.application.service;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;

    public CadastrarNovoOrcamentoService(OrcamentoRepository orcamentoRepository, ClienteRepository clienteRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void cadastrarNovoOrcamento(CadastrarNovoOrcamentoCommand command) {
        if (orcamentoRepository.buscarPorNumeroOrcamento(command.numeroOrcamento()).isPresent()) {
            throw new RegraDeNegocioException("Ja existe orcamento cadastrado com o mesmo numero.");
        }
        Cliente cliente = clienteRepository.buscarPorId(UUID.fromString(command.clienteId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));
        Orcamento orcamento = new Orcamento(
                command.numeroOrcamento(),
                cliente.getId(),
                command.ordemDeServicoId(),
                command.funcionarioId(),
                cliente.getNome(),
                cliente.getCpfOuCnpj(),
                command.placaVeiculo(),
                command.marcaVeiculo(),
                command.modeloVeiculo(),
                command.descricaoDiagnostico(),
                command.servicosPropostos(),
                command.pecasPrevistas(),
                command.valorMaoDeObra(),
                command.valorPecas(),
                LocalDateTime.now(),
                command.validade(),
                command.observacoes(),
                StatusOrcamento.AGUARDANDO_APROVACAO);

        orcamentoRepository.salvar(orcamento);
    }
}
