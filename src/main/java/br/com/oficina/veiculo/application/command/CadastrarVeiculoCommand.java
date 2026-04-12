package br.com.oficina.veiculo.application.command;

public record CadastrarVeiculoCommand(String placa, String marca, String modelo, String clienteId) {
}
