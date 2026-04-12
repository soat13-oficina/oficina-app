package br.com.oficina.application.orcamento;

public interface EnviarOrcamentoParaAprovacaoUseCase {
    void enviarOrcamentoParaAprovacao(EnviarOrcamentoParaAprovacaoRequest request);

    record EnviarOrcamentoParaAprovacaoRequest(String ordemDeServicoId) {
    }
}
