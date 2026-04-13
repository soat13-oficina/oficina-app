package br.com.oficina.orcamento.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import br.com.oficina.orcamento.application.command.CadastrarNovoOrcamentoCommand;
import br.com.oficina.orcamento.application.usecase.CadastrarNovoOrcamentoUseCase;
import br.com.oficina.orcamento.domain.model.Orcamento;
import br.com.oficina.orcamento.domain.model.StatusOrcamento;
import br.com.oficina.orcamento.domain.repository.OrcamentoRepository;

@Service
public class CadastrarNovoOrcamentoService implements CadastrarNovoOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public CadastrarNovoOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public void cadastrarNovoOrcamento(CadastrarNovoOrcamentoCommand command) {
        Orcamento orcamento = new Orcamento(
                command.numeroOrcamento(),
                command.ordemDeServicoId(),
                command.funcionarioId(),
                command.clienteNome(),
                command.clienteCpf(),
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
