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
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "clientes")
public class Cliente {
    @Id
    private String id;

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

    public Cliente(String id, String nome) {
        this(id, nome, null, null);
    }

    public Cliente(String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        this(null, nome, cpfOuCnpj, tipoCliente);
    }

    public Cliente(String id, String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        validarDocumento(cpfOuCnpj, tipoCliente);
        this.id = id;
        this.nome = nome;
        this.cpfOuCnpj = cpfOuCnpj;
        this.tipoCliente = tipoCliente;
    }

    public String getId() {
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

    @PrePersist
    void gerarIdSeNecessario() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }

    private static void validarDocumento(String cpfOuCnpj, TipoCliente tipoCliente) {
        if (cpfOuCnpj == null || tipoCliente == null) {
            return;
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
