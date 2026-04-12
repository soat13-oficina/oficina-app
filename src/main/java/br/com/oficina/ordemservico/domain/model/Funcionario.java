package br.com.oficina.ordemservico.domain.model;

public class Funcionario {
    private final String id;
    private final String nome;
    private final String cpf;

    public Funcionario(String id, String nome, String cpf) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }
}
