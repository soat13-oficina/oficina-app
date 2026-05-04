package br.com.oficina.ordemservico.domain.model;

import java.util.UUID;

import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "funcionarios")
public class Funcionario {
    private static final int QUANTIDADE_DIGITOS_CPF = 11;
    private static final String MENSAGEM_NOME_OBRIGATORIO = "Nome do funcionario e obrigatorio";
    private static final String MENSAGEM_CPF_INVALIDO = "CPF do funcionario deve possuir 11 digitos";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String cpf;

    protected Funcionario() {
    }

    public Funcionario(String nome, String cpf) {
        definirDados(nome, cpf);
    }

    public static Funcionario reconstituir(UUID id, String nome, String cpf) {
        Funcionario funcionario = new Funcionario();
        funcionario.id = id;
        funcionario.nome = nome;
        funcionario.cpf = cpf;
        return funcionario;
    }

    public void alterar(String nome, String cpf) {
        definirDados(nome, cpf);
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    private void definirDados(String nome, String cpf) {
        validarNome(nome);
        String cpfNormalizado = normalizarCpf(cpf);
        validarCpf(cpfNormalizado);
        this.nome = nome;
        this.cpf = cpfNormalizado;
    }

    private static String normalizarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        return cpf.replaceAll("\\D", "");
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException(MENSAGEM_NOME_OBRIGATORIO);
        }
    }

    private static void validarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return;
        }
        long digitosCpf = cpf.chars().filter(Character::isDigit).count();
        if (digitosCpf != QUANTIDADE_DIGITOS_CPF) {
            throw new RegraDeNegocioException(MENSAGEM_CPF_INVALIDO);
        }
    }
}
