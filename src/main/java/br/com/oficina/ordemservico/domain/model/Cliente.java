package br.com.oficina.ordemservico.domain.model;

import java.util.ArrayList;
import java.util.List;

public class Cliente {
    private final String id;
    private final String nome;
    private final List<Veiculo> veiculos = new ArrayList<>();
    private final List<OrdemDeServico> ordensDeServico = new ArrayList<>();

    public Cliente(String id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
