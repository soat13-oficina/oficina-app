package br.com.oficina.cliente.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.veiculo.domain.model.Veiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "clientes")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "cpf_ou_cnpj")
    private String cpfOuCnpj;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente")
    private TipoCliente tipoCliente;

    @Transient
    private final List<Veiculo> veiculos = new ArrayList<>();

    @Transient
    private final List<OrdemDeServico> ordensDeServico = new ArrayList<>();

    protected Cliente() {
    }

    public Cliente(String nome) {
        this(nome, null, null);
    }

    public Cliente(String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        validarDocumento(cpfOuCnpj, tipoCliente);
        this.nome = nome;
        this.cpfOuCnpj = cpfOuCnpj;
        this.tipoCliente = tipoCliente;
    }

    public static Cliente reconstituir(UUID id, String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        Cliente cliente = new Cliente(nome, cpfOuCnpj, tipoCliente);
        cliente.id = id;
        return cliente;
    }

    public static Cliente reconstituir(UUID id, String nome) {
        Cliente cliente = new Cliente(nome);
        cliente.id = id;
        return cliente;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpfOuCnpj() {
        return cpfOuCnpj;
    }

    public TipoCliente getTipoCliente() {
        return tipoCliente;
    }

    public void alterar(String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        validarDocumento(cpfOuCnpj, tipoCliente);
        this.nome = nome;
        this.cpfOuCnpj = cpfOuCnpj;
        this.tipoCliente = tipoCliente;
    }

    private static void validarDocumento(String cpfOuCnpj, TipoCliente tipoCliente) {
        if (cpfOuCnpj == null && tipoCliente == null) {
            return;
        }
        if (tipoCliente != null && (cpfOuCnpj == null || cpfOuCnpj.isBlank())) {
            throw new RegraDeNegocioException("Documento do cliente e obrigatorio quando o tipo for informado");
        }
        if ((cpfOuCnpj != null && !cpfOuCnpj.isBlank()) && tipoCliente == null) {
            throw new RegraDeNegocioException("Tipo do cliente e obrigatorio quando o documento for informado");
        }

        int quantidadeDeDigitos = contarDigitos(cpfOuCnpj);
        if (tipoCliente == TipoCliente.PF && quantidadeDeDigitos != 11) {
            throw new RegraDeNegocioException("CPF deve possuir 11 digitos");
        }
        if (tipoCliente == TipoCliente.PJ && quantidadeDeDigitos != 14) {
            throw new RegraDeNegocioException("CNPJ deve possuir 14 digitos");
        }
    }

    private static int contarDigitos(String valor) {
        return (int) valor.chars().filter(Character::isDigit).count();
    }
}
