package br.com.oficina.cliente.domain.model;

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
@Table(name = "clientes")
public class Cliente {
    private static final int QUANTIDADE_DIGITOS_CPF = 11;
    private static final int QUANTIDADE_DIGITOS_CNPJ = 14;
    private static final String MENSAGEM_NOME_OBRIGATORIO = "Nome do cliente e obrigatorio";
    private static final String MENSAGEM_DOCUMENTO_OBRIGATORIO = "Documento do cliente e obrigatorio quando o tipo for informado";
    private static final String MENSAGEM_TIPO_OBRIGATORIO = "Tipo do cliente e obrigatorio quando o documento for informado";
    private static final String MENSAGEM_CPF_INVALIDO = "CPF deve possuir 11 digitos";
    private static final String MENSAGEM_CNPJ_INVALIDO = "CNPJ deve possuir 14 digitos";

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

    @Column
    private String email;

    protected Cliente() {
    }

    public Cliente(String nome) {
        this(nome, null, null, null);
    }

    public Cliente(String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        this(nome, cpfOuCnpj, tipoCliente, null);
    }

    public Cliente(String nome, String cpfOuCnpj, TipoCliente tipoCliente, String email) {
        definirDados(nome, cpfOuCnpj, tipoCliente, email);
    }

    public static Cliente reconstituir(UUID id, String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        return reconstituir(id, nome, cpfOuCnpj, tipoCliente, null);
    }

    public static Cliente reconstituir(UUID id, String nome, String cpfOuCnpj, TipoCliente tipoCliente, String email) {
        Cliente cliente = new Cliente(nome, cpfOuCnpj, tipoCliente, email);
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

    public String getEmail() {
        return email;
    }

    public void alterar(String nome, String cpfOuCnpj, TipoCliente tipoCliente) {
        alterar(nome, cpfOuCnpj, tipoCliente, email);
    }

    public void alterar(String nome, String cpfOuCnpj, TipoCliente tipoCliente, String email) {
        definirDados(nome, cpfOuCnpj, tipoCliente, email);
    }

    private void definirDados(String nome, String cpfOuCnpj, TipoCliente tipoCliente, String email) {
        validarNome(nome);
        validarDocumento(cpfOuCnpj, tipoCliente);
        this.nome = nome;
        this.cpfOuCnpj = cpfOuCnpj;
        this.tipoCliente = tipoCliente;
        this.email = normalizarEmail(email);
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException(MENSAGEM_NOME_OBRIGATORIO);
        }
    }

    private static void validarDocumento(String cpfOuCnpj, TipoCliente tipoCliente) {
        if (!documentoInformado(cpfOuCnpj) && tipoCliente == null) {
            return;
        }
        if (tipoCliente != null && !documentoInformado(cpfOuCnpj)) {
            throw new RegraDeNegocioException(MENSAGEM_DOCUMENTO_OBRIGATORIO);
        }
        if (documentoInformado(cpfOuCnpj) && tipoCliente == null) {
            throw new RegraDeNegocioException(MENSAGEM_TIPO_OBRIGATORIO);
        }

        int quantidadeDeDigitos = contarDigitos(cpfOuCnpj);
        if (tipoCliente == TipoCliente.PF && quantidadeDeDigitos != QUANTIDADE_DIGITOS_CPF) {
            throw new RegraDeNegocioException(MENSAGEM_CPF_INVALIDO);
        }
        if (tipoCliente == TipoCliente.PJ && quantidadeDeDigitos != QUANTIDADE_DIGITOS_CNPJ) {
            throw new RegraDeNegocioException(MENSAGEM_CNPJ_INVALIDO);
        }
    }

    private static boolean documentoInformado(String cpfOuCnpj) {
        return cpfOuCnpj != null && !cpfOuCnpj.isBlank();
    }

    private static int contarDigitos(String valor) {
        return (int) valor.chars().filter(Character::isDigit).count();
    }

    private static String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim();
    }
}
