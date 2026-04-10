package br.com.oficina.ordemservico.application.usecase;

public interface SolicitarOrcamentoDeServicoUseCase {
    void solicitarOrcamentoDeServico(SolicitarOrcamentoDeServicoRequest request);

    record SolicitarOrcamentoDeServicoRequest(String funcionarioId, String clienteId, String placaVeiculo) {
    }
}
