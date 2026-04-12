package br.com.oficina.application.veiculo;

public record CadastrarVeiculoRequest(String placa, String marca, String modelo, String clienteId) {
}
