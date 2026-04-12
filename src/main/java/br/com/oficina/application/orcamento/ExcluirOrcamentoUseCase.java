package br.com.oficina.application.orcamento;

public interface ExcluirOrcamentoUseCase {
    void excluirOrcamento(ExcluirOrcamentoRequest request);

    record ExcluirOrcamentoRequest(String orcamentoId) {
    }
}
