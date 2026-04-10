package br.com.oficina.ordemservico.domain.model;

public class Veiculo {
    private final String placa;
    private final String marca;
    private final String modelo;

    public Veiculo(String placa, String marca, String modelo) {
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
    }

    public String getPlaca() {
        return placa;
    }
}
