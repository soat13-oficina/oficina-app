package br.com.oficina.orcamento.application.usecase;

public interface GerarOrcamentoUseCase {
    void gerarOrcamento(GerarOrcamentoRequest request);

    record GerarOrcamentoRequest(String ordemDeServicoId) {
    }
}
