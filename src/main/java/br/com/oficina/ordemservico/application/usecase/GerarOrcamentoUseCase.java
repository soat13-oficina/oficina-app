package br.com.oficina.ordemservico.application.usecase;

public interface GerarOrcamentoUseCase {
    void gerarOrcamento(GerarOrcamentoRequest request);

    record GerarOrcamentoRequest(String ordemDeServicoId) {
    }
}
