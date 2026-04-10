package br.com.oficina.ordemservico.application.usecase;

public interface CadastrarVeiculoUseCase {
    void cadastrarVeiculo(CadastrarVeiculoRequest request);

    record CadastrarVeiculoRequest(String placa, String marca, String modelo, String clienteId) {
    }
}
