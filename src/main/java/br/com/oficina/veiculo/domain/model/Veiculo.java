package br.com.oficina.veiculo.domain.model;

public class Veiculo {
    private final String placa;
    private final String marca;
    private final String modelo;
    private final String fabricante;
    private final int ano;
    private final int potencia;
    private final String cambio;
    private final TipoCombustivel tipo;

    public Veiculo(
            String placa,
            String marca,
            String modelo,
            String fabricante,
            int ano,
            int potencia,
            String cambio,
            TipoCombustivel tipo) {
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.ano = ano;
        this.potencia = potencia;
        this.cambio = cambio;
        this.tipo = tipo;
    }

    public String getPlaca() {
        return placa;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public String getFabricante() {
        return fabricante;
    }

    public int getAno() {
        return ano;
    }

    public int getPotencia() {
        return potencia;
    }

    public String getCambio() {
        return cambio;
    }

    public TipoCombustivel getTipo() {
        return tipo;
    }
}
