package br.com.oficina.ordemservico.application.query;

public record ConsultarOrdensDeServicoQuery(
        String numeroOrdemServico,
        String nomeCliente,
        String placaVeiculo,
        String cpfCliente) {
}
