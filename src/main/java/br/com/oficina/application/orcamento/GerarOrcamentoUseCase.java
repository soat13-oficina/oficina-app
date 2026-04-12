package br.com.oficina.application.orcamento;

public interface GerarOrcamentoUseCase {
    void gerarOrcamento(GerarOrcamentoRequest request);

    record GerarOrcamentoRequest(String ordemDeServicoId) {
    }
}
