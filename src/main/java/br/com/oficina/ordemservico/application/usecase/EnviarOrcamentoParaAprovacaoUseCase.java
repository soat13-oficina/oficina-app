package br.com.oficina.ordemservico.application.usecase;

public interface EnviarOrcamentoParaAprovacaoUseCase {
    void enviarOrcamentoParaAprovacao(EnviarOrcamentoParaAprovacaoRequest request);

    record EnviarOrcamentoParaAprovacaoRequest(String ordemDeServicoId) {
    }
}
