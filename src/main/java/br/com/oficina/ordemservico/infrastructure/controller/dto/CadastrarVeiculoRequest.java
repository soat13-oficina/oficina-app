package br.com.oficina.ordemservico.infrastructure.controller.dto;

public record CadastrarVeiculoRequest(String placa, String marca, String modelo, String clienteId) {
}
