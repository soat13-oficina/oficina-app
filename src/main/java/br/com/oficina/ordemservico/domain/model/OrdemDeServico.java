package br.com.oficina.ordemservico.domain.model;

import java.util.UUID;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ordens_de_servico")
public class OrdemDeServico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String numeroOrdemServico;

    @Column(nullable = false)
    private String funcionarioId;

    @Column(nullable = false)
    private String funcionarioNome;

    private String funcionarioCpf;

    @Column(nullable = false)
    private UUID clienteId;

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

    protected OrdemDeServico() {
    }

    private OrdemDeServico(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            StatusOrdemDeServico status) {
        this.id = id;
        this.numeroOrdemServico = numeroOrdemServico;
        this.status = status;
        definirDados(funcionario, cliente, veiculo);
    }

    public static OrdemDeServico abrir(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo) {
        return new OrdemDeServico(
                id,
                numeroOrdemServico,
                funcionario,
                cliente,
                veiculo,
                StatusOrdemDeServico.OS_ABERTA);
    }

    public static OrdemDeServico reconstituir(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            StatusOrdemDeServico status) {
        return new OrdemDeServico(id, numeroOrdemServico, funcionario, cliente, veiculo, status);
    }

    public void iniciarDiagnostico() {
        if (status != StatusOrdemDeServico.OS_ABERTA) {
            throw new RegraDeNegocioException("Diagnostico so pode ser iniciado para ordem aberta");
        }
        status = StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO;
    }

    public void alterar(Funcionario funcionario, Cliente cliente, Veiculo veiculo) {
        if (status != StatusOrdemDeServico.OS_ABERTA) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser alterada enquanto estiver aberta");
        }
        definirDados(funcionario, cliente, veiculo);
    }

    public void concluirDiagnostico() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Diagnostico so pode ser concluido em andamento");
        }
        status = StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO;
    }

    public void finalizar() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser finalizada com diagnostico concluido");
        }
        status = StatusOrdemDeServico.OS_FINALIZADA;
    }

    public UUID getId() {
        return id;
    }

    public String getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public Funcionario getFuncionario() {
        return new Funcionario(funcionarioId, funcionarioNome, funcionarioCpf);
    }

    public Cliente getCliente() {
        return Cliente.reconstituir(clienteId, clienteNome, clienteDocumento, clienteTipo);
    }

    public Veiculo getVeiculo() {
        return new Veiculo(
                clienteId,
                veiculoPlaca,
                veiculoMarca,
                veiculoModelo,
                veiculoFabricante,
                veiculoAno,
                veiculoPotencia,
                veiculoCambio,
                veiculoTipo);
    }

    public StatusOrdemDeServico getStatus() {
        return status;
    }

    private void definirDados(Funcionario funcionario, Cliente cliente, Veiculo veiculo) {
        this.funcionarioId = funcionario.getId();
        this.funcionarioNome = funcionario.getNome();
        this.funcionarioCpf = funcionario.getCpf();
        this.clienteId = cliente.getId();
        this.clienteNome = cliente.getNome();
        this.clienteDocumento = cliente.getCpfOuCnpj();
        this.clienteTipo = cliente.getTipoCliente();
        this.veiculoPlaca = veiculo.getPlaca();
        this.veiculoMarca = veiculo.getMarca();
        this.veiculoModelo = veiculo.getModelo();
        this.veiculoFabricante = veiculo.getFabricante();
        this.veiculoAno = veiculo.getAno();
        this.veiculoPotencia = veiculo.getPotencia();
        this.veiculoCambio = veiculo.getCambio();
        this.veiculoTipo = veiculo.getTipo();
    }
}
