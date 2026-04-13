package br.com.oficina.ordemservico.infrastructure.persistence;

import java.util.UUID;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.ordemservico.domain.model.Funcionario;
import br.com.oficina.ordemservico.domain.model.OrdemDeServico;
import br.com.oficina.ordemservico.domain.model.StatusOrdemDeServico;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ordens_de_servico")
public class OrdemDeServicoJpaEntity {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String numeroOrdemServico;

    @Column(nullable = false)
    private String funcionarioId;

    @Column(nullable = false)
    private String funcionarioNome;

    private String funcionarioCpf;

    @Column(nullable = false)
    private String clienteId;

    @Column(nullable = false)
    private String clienteNome;

    private String clienteDocumento;

    @Enumerated(EnumType.STRING)
    private TipoCliente clienteTipo;

    @Column(nullable = false)
    private String veiculoPlaca;

    @Column(nullable = false)
    private String veiculoMarca;

    @Column(nullable = false)
    private String veiculoModelo;

    @Column(nullable = false)
    private String veiculoFabricante;

    @Column(nullable = false)
    private int veiculoAno;

    @Column(nullable = false)
    private int veiculoPotencia;

    @Column(nullable = false)
    private String veiculoCambio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCombustivel veiculoTipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOrdemDeServico status;

    protected OrdemDeServicoJpaEntity() {
    }

    private OrdemDeServicoJpaEntity(
            String id,
            String numeroOrdemServico,
            String funcionarioId,
            String funcionarioNome,
            String funcionarioCpf,
            String clienteId,
            String clienteNome,
            String clienteDocumento,
            TipoCliente clienteTipo,
            String veiculoPlaca,
            String veiculoMarca,
            String veiculoModelo,
            String veiculoFabricante,
            int veiculoAno,
            int veiculoPotencia,
            String veiculoCambio,
            TipoCombustivel veiculoTipo,
            StatusOrdemDeServico status) {
        this.id = id;
        this.numeroOrdemServico = numeroOrdemServico;
        this.funcionarioId = funcionarioId;
        this.funcionarioNome = funcionarioNome;
        this.funcionarioCpf = funcionarioCpf;
        this.clienteId = clienteId;
        this.clienteNome = clienteNome;
        this.clienteDocumento = clienteDocumento;
        this.clienteTipo = clienteTipo;
        this.veiculoPlaca = veiculoPlaca;
        this.veiculoMarca = veiculoMarca;
        this.veiculoModelo = veiculoModelo;
        this.veiculoFabricante = veiculoFabricante;
        this.veiculoAno = veiculoAno;
        this.veiculoPotencia = veiculoPotencia;
        this.veiculoCambio = veiculoCambio;
        this.veiculoTipo = veiculoTipo;
        this.status = status;
    }

    public static OrdemDeServicoJpaEntity fromDomain(OrdemDeServico ordemDeServico) {
        return new OrdemDeServicoJpaEntity(
                ordemDeServico.getId(),
                ordemDeServico.getNumeroOrdemServico(),
                ordemDeServico.getFuncionario().getId(),
                ordemDeServico.getFuncionario().getNome(),
                ordemDeServico.getFuncionario().getCpf(),
                ordemDeServico.getCliente().getId().toString(),
                ordemDeServico.getCliente().getNome(),
                ordemDeServico.getCliente().getCpfOuCnpj(),
                ordemDeServico.getCliente().getTipoCliente(),
                ordemDeServico.getVeiculo().getPlaca(),
                ordemDeServico.getVeiculo().getMarca(),
                ordemDeServico.getVeiculo().getModelo(),
                ordemDeServico.getVeiculo().getFabricante(),
                ordemDeServico.getVeiculo().getAno(),
                ordemDeServico.getVeiculo().getPotencia(),
                ordemDeServico.getVeiculo().getCambio(),
                ordemDeServico.getVeiculo().getTipo(),
                ordemDeServico.getStatus());
    }

    public OrdemDeServico toDomain() {
        Funcionario funcionario = new Funcionario(funcionarioId, funcionarioNome, funcionarioCpf);
        Cliente cliente = Cliente.reconstituir(UUID.fromString(clienteId), clienteNome, clienteDocumento, clienteTipo);
        Veiculo veiculo = new Veiculo(
                cliente.getId(),
                veiculoPlaca,
                veiculoMarca,
                veiculoModelo,
                veiculoFabricante,
                veiculoAno,
                veiculoPotencia,
                veiculoCambio,
                veiculoTipo);
        return OrdemDeServico.reconstituir(id, numeroOrdemServico, funcionario, cliente, veiculo, status);
    }
}
