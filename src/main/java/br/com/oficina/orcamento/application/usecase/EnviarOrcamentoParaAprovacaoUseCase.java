package br.com.oficina.orcamento.application.usecase;

public interface EnviarOrcamentoParaAprovacaoUseCase {
    void enviarOrcamentoParaAprovacao(EnviarOrcamentoParaAprovacaoRequest request);

    record EnviarOrcamentoParaAprovacaoRequest(String ordemDeServicoId) {
    }
}
