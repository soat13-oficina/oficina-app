package br.com.oficina.domain.model.cliente;

import java.util.ArrayList;
import java.util.List;

import br.com.oficina.domain.model.ordemservico.OrdemDeServico;
import br.com.oficina.domain.model.veiculo.Veiculo;

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
