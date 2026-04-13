package br.com.oficina.orcamento.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.repository.ClienteRepository;
import br.com.oficina.common.domain.exception.RecursoNaoEncontradoException;
import br.com.oficina.orcamento.application.command.AlterarOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.AlterarOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class AlterarOrcamentoService implements AlterarOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;

    public AlterarOrcamentoService(OrcamentoRepository orcamentoRepository, ClienteRepository clienteRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public void alterarOrcamento(AlterarOrcamentoCommand command) {
        Orcamento orcamentoAtual = orcamentoRepository.buscarPorNumeroOrcamento(command.numeroOrcamento())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orcamento nao encontrado para o numero informado."));
        Cliente cliente = clienteRepository.buscarPorId(UUID.fromString(command.clienteId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado para o identificador informado."));

        Orcamento orcamentoAtualizado = Orcamento.reconstituir(
                orcamentoAtual.getId(),
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
                orcamentoAtual.getCriadoEm(),
                command.validade(),
                command.observacoes(),
                orcamentoAtual.getStatus());

        if (orcamentoAtual.getEnviadoParaAprovacaoEm() != null) {
            orcamentoAtualizado.enviarParaAprovacao(orcamentoAtual.getEnviadoParaAprovacaoEm());
        }
        if (orcamentoAtual.getStatus() == br.com.oficina.orcamento.domain.model.StatusOrcamento.APROVADO) {
            orcamentoAtualizado.aprovar();
        }
        if (orcamentoAtual.getStatus() == br.com.oficina.orcamento.domain.model.StatusOrcamento.REJEITADO) {
            orcamentoAtualizado.rejeitar();
        }

        orcamentoRepository.atualizar(orcamentoAtualizado);
    }
}
