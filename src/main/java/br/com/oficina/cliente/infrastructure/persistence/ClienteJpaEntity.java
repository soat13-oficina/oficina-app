package br.com.oficina.cliente.infrastructure.persistence;

import br.com.oficina.cliente.domain.model.Cliente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class ClienteJpaEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String nome;

    private String cpf;

    protected ClienteJpaEntity() {
    }

    private ClienteJpaEntity(String id, String nome, String cpf) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
    }

    public static ClienteJpaEntity fromDomain(Cliente cliente) {
        return new ClienteJpaEntity(cliente.getId(), cliente.getNome(), cliente.getCpf());
    }

    public Cliente toDomain() {
        return new Cliente(id, nome, cpf);
    }
}
