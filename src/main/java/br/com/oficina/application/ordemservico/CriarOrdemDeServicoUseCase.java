package br.com.oficina.application.ordemservico;

public interface CriarOrdemDeServicoUseCase {
    void criarOrdemDeServico(CriarOrdemDeServicoRequest request);

    record CriarOrdemDeServicoRequest(String clienteId, String funcionarioId, String placaVeiculo) {
    }
}
