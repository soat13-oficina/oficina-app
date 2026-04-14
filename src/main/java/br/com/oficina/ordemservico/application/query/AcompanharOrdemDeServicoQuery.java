package br.com.oficina.ordemservico.application.query;

public record AcompanharOrdemDeServicoQuery(
        String numeroOrdemServico,
        String documentoCliente) {
}
