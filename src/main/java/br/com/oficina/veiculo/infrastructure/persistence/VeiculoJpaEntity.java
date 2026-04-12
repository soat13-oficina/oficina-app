package br.com.oficina.veiculo.infrastructure.persistence;

import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "veiculos")
public class VeiculoJpaEntity {
    @Id
    private String placa;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private String fabricante;

    @Column(nullable = false)
    private int ano;

    @Column(nullable = false)
    private int potencia;

    @Column(nullable = false)
    private String cambio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCombustivel tipo;

    protected VeiculoJpaEntity() {
    }

    private VeiculoJpaEntity(
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

    public static VeiculoJpaEntity fromDomain(Veiculo veiculo) {
        return new VeiculoJpaEntity(
                veiculo.getPlaca(),
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getFabricante(),
                veiculo.getAno(),
                veiculo.getPotencia(),
                veiculo.getCambio(),
                veiculo.getTipo());
    }

    public Veiculo toDomain() {
        return new Veiculo(placa, marca, modelo, fabricante, ano, potencia, cambio, tipo);
    }
}
