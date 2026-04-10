package br.com.oficina.ordemservico.application.usecase;

public interface CriarOrdemDeServicoUseCase {
    void criarOrdemDeServico(CriarOrdemDeServicoRequest request);

    record CriarOrdemDeServicoRequest(String clienteId, String funcionarioId, String placaVeiculo) {
    }
}
