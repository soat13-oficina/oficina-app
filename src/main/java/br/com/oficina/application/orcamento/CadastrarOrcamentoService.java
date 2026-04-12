package br.com.oficina.application.orcamento;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import br.com.oficina.domain.model.orcamento.Orcamento;
import br.com.oficina.domain.repository.OrcamentoRepository;

@Service
public class CadastrarOrcamentoService implements CadastrarOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public CadastrarOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public void cadastrarOrcamento(CadastrarOrcamentoRequest request) {
        Orcamento orcamento = new Orcamento(
                request.id(),
                request.ordemDeServicoId(),
                request.funcionarioId(),
                request.clienteId(),
                request.placaVeiculo(),
                request.descricaoDiagnostico(),
                request.servicosPropostos(),
                request.pecasPrevistas(),
                request.valorMaoDeObra(),
                request.valorPecas(),
                LocalDateTime.now(),
                request.validade(),
                request.observacoes());

        orcamentoRepository.salvar(orcamento);
    }
}
