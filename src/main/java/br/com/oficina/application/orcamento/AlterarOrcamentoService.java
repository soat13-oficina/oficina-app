package br.com.oficina.application.orcamento;

import org.springframework.stereotype.Service;

import br.com.oficina.domain.model.orcamento.Orcamento;
import br.com.oficina.domain.repository.OrcamentoRepository;

@Service
public class AlterarOrcamentoService implements AlterarOrcamentoUseCase {
    private final OrcamentoRepository orcamentoRepository;

    public AlterarOrcamentoService(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public void alterarOrcamento(AlterarOrcamentoRequest request) {
        Orcamento orcamentoAtual = orcamentoRepository.buscarPorId(request.orcamentoId())
                .orElseThrow(() -> new IllegalArgumentException("Orcamento nao encontrado"));

        Orcamento orcamentoAtualizado = new Orcamento(
                request.orcamentoId(),
                request.ordemDeServicoId(),
                request.funcionarioId(),
                request.clienteId(),
                request.placaVeiculo(),
                request.descricaoDiagnostico(),
                request.servicosPropostos(),
                request.pecasPrevistas(),
                request.valorMaoDeObra(),
                request.valorPecas(),
                orcamentoAtual.getCriadoEm(),
                request.validade(),
                request.observacoes());

        if (orcamentoAtual.getEnviadoParaAprovacaoEm() != null) {
            orcamentoAtualizado.enviarParaAprovacao(orcamentoAtual.getEnviadoParaAprovacaoEm());
        }

        orcamentoRepository.atualizar(orcamentoAtualizado);
    }
}
