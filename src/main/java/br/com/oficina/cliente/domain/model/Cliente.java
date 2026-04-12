package br.com.oficina.cliente.domain.model;

import java.util.ArrayList;
import java.util.List;

import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.veiculo.domain.model.Veiculo;

public class Cliente {
    private final String id;
    private final String nome;
    private final String cpf;
    private final List<Veiculo> veiculos = new ArrayList<>();
    private final List<OrdemDeServico> ordensDeServico = new ArrayList<>();

    public Cliente(String id, String nome) {
        this(id, nome, null);
    }

    public Cliente(String id, String nome, String cpf) {
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
