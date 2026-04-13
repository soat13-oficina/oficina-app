package br.com.oficina.orcamento.application.query;

public record ConsultarOrcamentoQuery(
        String numeroOrcamento,
        String cpfCliente,
        String placaVeiculo) {
}
