package br.com.oficina.veiculo.domain.model;

import java.time.Year;
import java.util.Locale;
import java.util.UUID;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "veiculos")
public class Veiculo {
    private static final String MENSAGEM_PLACA_OBRIGATORIA = "Placa do veiculo e obrigatoria";
    private static final String MENSAGEM_PLACA_INVALIDA = "Placa do veiculo invalida";
    private static final String MENSAGEM_POTENCIA_INVALIDA = "Potencia do veiculo deve ser maior que zero.";
    private static final String FORMATO_ANTIGO = "^[A-Z]{3}\\d{4}$";
    private static final String FORMATO_MERCOSUL = "^[A-Z]{3}\\d[A-Z]\\d{2}$";
    private static final int ANO_MINIMO = 1886;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID clienteId;

    @Column(nullable = false, unique = true)
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

    protected Veiculo() {
    }

    public Veiculo(
            String placa,
            String marca,
            String modelo,
            String fabricante,
            int ano,
            int potencia,
            String cambio,
            TipoCombustivel tipo) {
        this(null, placa, marca, modelo, fabricante, ano, potencia, cambio, tipo);
    }

    public Veiculo(
            UUID clienteId,
            String placa,
            String marca,
            String modelo,
            String fabricante,
            int ano,
            int potencia,
            String cambio,
            TipoCombustivel tipo) {
        validarAno(ano);
        validarPotencia(potencia);
        this.clienteId = clienteId;
        this.placa = normalizarPlaca(placa);
        this.marca = marca;
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.ano = ano;
        this.potencia = potencia;
        this.cambio = cambio;
        this.tipo = tipo;
    }

    public static Veiculo reconstituir(
            UUID id,
            UUID clienteId,
            String placa,
            String marca,
            String modelo,
            String fabricante,
            int ano,
            int potencia,
            String cambio,
            TipoCombustivel tipo) {
        Veiculo veiculo = new Veiculo(clienteId, placa, marca, modelo, fabricante, ano, potencia, cambio, tipo);
        veiculo.id = id;
        return veiculo;
    }

    public static Veiculo reconstituir(
            UUID id,
            String placa,
            String marca,
            String modelo,
            String fabricante,
            int ano,
            int potencia,
            String cambio,
            TipoCombustivel tipo) {
        return reconstituir(id, null, placa, marca, modelo, fabricante, ano, potencia, cambio, tipo);
    }

    public void alterar(
            String marca,
            String modelo,
            String fabricante,
            int ano,
            int potencia,
            String cambio,
            TipoCombustivel tipo) {
        validarAno(ano);
        validarPotencia(potencia);
        this.marca = marca;
        this.modelo = modelo;
        this.fabricante = fabricante;
        this.ano = ano;
        this.potencia = potencia;
        this.cambio = cambio;
        this.tipo = tipo;
    }

    private static void validarAno(int ano) {
        int anoMaximo = Year.now().getValue() + 1;
        if (ano < ANO_MINIMO || ano > anoMaximo) {
            throw new RegraDeNegocioException(
                    "Ano do veiculo deve estar entre " + ANO_MINIMO + " e " + anoMaximo + ".");
        }
    }

    private static void validarPotencia(int potencia) {
        if (potencia <= 0) {
            throw new RegraDeNegocioException(MENSAGEM_POTENCIA_INVALIDA);
        }
    }

    public static String normalizarPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new RegraDeNegocioException(MENSAGEM_PLACA_OBRIGATORIA);
        }

        String placaNormalizada = placa.replace("-", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT)
                .trim();

        if (!placaNormalizada.matches(FORMATO_ANTIGO) && !placaNormalizada.matches(FORMATO_MERCOSUL)) {
            throw new RegraDeNegocioException(MENSAGEM_PLACA_INVALIDA);
        }

        return placaNormalizada;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
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
