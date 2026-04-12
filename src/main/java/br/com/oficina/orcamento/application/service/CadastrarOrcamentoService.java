package br.com.oficina.orcamento.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import br.com.oficina.orcamento.application.command.CadastrarOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.CadastrarOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class CadastrarOrcamentoService implements CadastrarOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public CadastrarOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public void cadastrarOrcamento(CadastrarOrcamentoCommand command) {
        Orcamento orcamento = new Orcamento(
                command.id(),
                command.ordemDeServicoId(),
                command.funcionarioId(),
                command.clienteId(),
                command.placaVeiculo(),
                command.descricaoDiagnostico(),
                command.servicosPropostos(),
                command.pecasPrevistas(),
                command.valorMaoDeObra(),
                command.valorPecas(),
                LocalDateTime.now(),
                command.validade(),
                command.observacoes());

        orcamentoRepository.salvar(orcamento);
    }
}
