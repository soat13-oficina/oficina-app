package br.com.oficina.veiculo.infrastructure.web.request;

public record CadastrarVeiculoRequest(String placa, String marca, String modelo, String clienteId) {
}
