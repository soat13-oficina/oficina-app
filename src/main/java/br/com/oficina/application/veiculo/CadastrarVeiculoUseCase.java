package br.com.oficina.application.veiculo;

public interface CadastrarVeiculoUseCase {
    void cadastrarVeiculo(CadastrarVeiculoRequest request);

    record CadastrarVeiculoRequest(String placa, String marca, String modelo, String clienteId) {
    }
}
