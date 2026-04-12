package br.com.oficina.orcamento.application.service;

import org.springframework.stereotype.Service;

import br.com.oficina.orcamento.application.command.AlterarOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.AlterarOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class AlterarOrcamentoService implements AlterarOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public AlterarOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public void alterarOrcamento(AlterarOrcamentoCommand command) {
        Orcamento orcamentoAtual = orcamentoRepository.buscarPorId(command.orcamentoId())
                .orElseThrow(() -> new IllegalArgumentException("Orcamento nao encontrado"));

        Orcamento orcamentoAtualizado = new Orcamento(
                command.orcamentoId(),
                command.ordemDeServicoId(),
                command.funcionarioId(),
                command.clienteId(),
                command.placaVeiculo(),
                command.descricaoDiagnostico(),
                command.servicosPropostos(),
                command.pecasPrevistas(),
                command.valorMaoDeObra(),
                command.valorPecas(),
                orcamentoAtual.getCriadoEm(),
                command.validade(),
                command.observacoes());

        if (orcamentoAtual.getEnviadoParaAprovacaoEm() != null) {
            orcamentoAtualizado.enviarParaAprovacao(orcamentoAtual.getEnviadoParaAprovacaoEm());
        }

        orcamentoRepository.atualizar(orcamentoAtualizado);
    }
}
