package br.com.oficina.ordemservico.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import br.com.oficina.cliente.domain.model.Cliente;
import br.com.oficina.cliente.domain.model.TipoCliente;
import br.com.oficina.common.domain.exception.RegraDeNegocioException;
import br.com.oficina.veiculo.domain.model.TipoCombustivel;
import br.com.oficina.veiculo.domain.model.Veiculo;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
    private UUID funcionarioId;

    @Column(nullable = false)
    private String funcionarioNome;

    private String funcionarioCpf;

    @Column(nullable = false)
    private UUID clienteId;

    @Column(nullable = false)
    private UUID veiculoId;

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

    private LocalDateTime iniciadaEm;

    private LocalDateTime finalizadaEm;

    private LocalDateTime entregueEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_encerramento")
    private MotivoEncerramento motivoEncerramento;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ordem_servico_servicos",
            joinColumns = @JoinColumn(name = "ordem_de_servico_id"))
    private List<ServicoOrdem> servicos = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ordem_servico_pecas",
            joinColumns = @JoinColumn(name = "ordem_de_servico_id"))
    private List<PecaPrevistaOrdem> pecasPrevistas = new ArrayList<>();

    protected OrdemDeServico() {
    }

    private OrdemDeServico(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            StatusOrdemDeServico status,
            LocalDateTime iniciadaEm,
            LocalDateTime finalizadaEm,
            LocalDateTime entregueEm) {
        this.id = id;
        this.numeroOrdemServico = numeroOrdemServico;
        this.status = status;
        this.iniciadaEm = iniciadaEm;
        this.finalizadaEm = finalizadaEm;
        this.entregueEm = entregueEm;
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
                StatusOrdemDeServico.OS_ABERTA,
                null,
                null,
                null);
    }

    public static OrdemDeServico abrir(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            List<ServicoOrdem> servicos,
            List<PecaPrevistaOrdem> pecasPrevistas) {
        OrdemDeServico ordemDeServico = abrir(id, numeroOrdemServico, funcionario, cliente, veiculo);
        if (servicos != null) {
            ordemDeServico.servicos.addAll(servicos);
        }
        if (pecasPrevistas != null) {
            ordemDeServico.pecasPrevistas.addAll(pecasPrevistas);
        }
        return ordemDeServico;
    }

    public static OrdemDeServico reconstituir(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            StatusOrdemDeServico status,
            LocalDateTime iniciadaEm,
            LocalDateTime finalizadaEm,
            LocalDateTime entregueEm) {
        return new OrdemDeServico(
                id,
                numeroOrdemServico,
                funcionario,
                cliente,
                veiculo,
                status,
                iniciadaEm,
                finalizadaEm,
                entregueEm);
    }

    public static OrdemDeServico reconstituir(
            UUID id,
            String numeroOrdemServico,
            Funcionario funcionario,
            Cliente cliente,
            Veiculo veiculo,
            StatusOrdemDeServico status,
            LocalDateTime iniciadaEm,
            LocalDateTime finalizadaEm,
            LocalDateTime entregueEm,
            MotivoEncerramento motivoEncerramento,
            List<ServicoOrdem> servicos,
            List<PecaPrevistaOrdem> pecasPrevistas) {
        OrdemDeServico ordemDeServico = reconstituir(
                id,
                numeroOrdemServico,
                funcionario,
                cliente,
                veiculo,
                status,
                iniciadaEm,
                finalizadaEm,
                entregueEm);
        ordemDeServico.motivoEncerramento = motivoEncerramento;
        if (servicos != null) {
            ordemDeServico.servicos.addAll(servicos);
        }
        if (pecasPrevistas != null) {
            ordemDeServico.pecasPrevistas.addAll(pecasPrevistas);
        }
        return ordemDeServico;
    }

    public void iniciarDiagnostico() {
        if (status != StatusOrdemDeServico.OS_ABERTA) {
            throw new RegraDeNegocioException("Diagnostico so pode ser iniciado para ordem aberta");
        }
        status = StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO;
        iniciadaEm = LocalDateTime.now();
    }

    public void alterar(Cliente cliente, Veiculo veiculo) {
        if (status != StatusOrdemDeServico.OS_ABERTA) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser alterada enquanto estiver aberta");
        }
        definirDados(getFuncionario(), cliente, veiculo);
    }

    public void concluirDiagnostico() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Diagnostico so pode ser concluido em andamento");
        }
        status = StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO;
    }

    public void concluirDiagnostico(String descricaoServico, List<PecaPrevistaOrdem> pecas) {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Diagnostico so pode ser concluido em andamento");
        }
        if (descricaoServico == null || descricaoServico.isBlank()) {
            throw new RegraDeNegocioException("Descricao do servico e obrigatoria no fechamento do diagnostico");
        }
        // Mao de obra nao e definida no fechamento (e financeiro do orcamento): registra apenas a descricao.
        servicos.add(new ServicoOrdem(descricaoServico, null));
        if (pecas != null) {
            pecasPrevistas.addAll(pecas);
        }
        status = StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO;
    }

    public void enviarParaOrcamento() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO) {
            throw new RegraDeNegocioException("Diagnostico so pode ser enviado para orcamento quando concluido");
        }
        status = StatusOrdemDeServico.ORCAMENTO_GERADO;
    }

    public void enviarParaAprovacao() {
        if (status != StatusOrdemDeServico.DIAGNOSTICO_CONCLUIDO) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser enviada para aprovacao com diagnostico concluido");
        }
        status = StatusOrdemDeServico.AGUARDANDO_APROVACAO;
    }

    public void iniciarExecucao() {
        if (status != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Execucao so pode iniciar quando a ordem estiver aguardando aprovacao");
        }
        status = StatusOrdemDeServico.SERVICO_EM_ANDAMENTO;
    }

    public void concluirServico() {
        if (status != StatusOrdemDeServico.SERVICO_EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Servico so pode ser concluido quando estiver em execucao");
        }
        status = StatusOrdemDeServico.OS_FINALIZADA;
        motivoEncerramento = MotivoEncerramento.SERVICO_CONCLUIDO;
        finalizadaEm = LocalDateTime.now();
    }

    public void recusarOrcamento() {
        if (status != StatusOrdemDeServico.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("Orcamento so pode ser recusado quando a ordem estiver aguardando aprovacao");
        }
        status = StatusOrdemDeServico.OS_FINALIZADA;
        motivoEncerramento = MotivoEncerramento.ORCAMENTO_RECUSADO;
        finalizadaEm = LocalDateTime.now();
    }

    public void finalizar() {
        if (status != StatusOrdemDeServico.ORCAMENTO_GERADO) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser finalizada com orcamento gerado");
        }
        status = StatusOrdemDeServico.OS_FINALIZADA;
        finalizadaEm = LocalDateTime.now();
    }

    public void entregarAoCliente() {
        if (status != StatusOrdemDeServico.OS_FINALIZADA) {
            throw new RegraDeNegocioException("Ordem de servico so pode ser entregue ao cliente quando estiver finalizada");
        }
        status = StatusOrdemDeServico.ENTREGUE;
        entregueEm = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public Funcionario getFuncionario() {
        return Funcionario.reconstituir(funcionarioId, funcionarioNome, funcionarioCpf);
    }

    public Cliente getCliente() {
        return Cliente.reconstituir(clienteId, clienteNome, clienteDocumento, clienteTipo);
    }

    public UUID getVeiculoId() {
        return veiculoId;
    }

    public Veiculo getVeiculo() {
        return Veiculo.reconstituir(
                veiculoId,
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

    public LocalDateTime getIniciadaEm() {
        return iniciadaEm;
    }

    public LocalDateTime getFinalizadaEm() {
        return finalizadaEm;
    }

    public LocalDateTime getEntregueEm() {
        return entregueEm;
    }

    public SituacaoOrdemDeServico getSituacao() {
        return SituacaoOrdemDeServico.fromStatus(status);
    }

    public MotivoEncerramento getMotivoEncerramento() {
        return motivoEncerramento;
    }

    public List<ServicoOrdem> getServicos() {
        return Collections.unmodifiableList(servicos);
    }

    public List<PecaPrevistaOrdem> getPecasPrevistas() {
        return Collections.unmodifiableList(pecasPrevistas);
    }

    private void definirDados(Funcionario funcionario, Cliente cliente, Veiculo veiculo) {
        if (cliente.getId() == null) {
            throw new RegraDeNegocioException("Cliente da ordem de servico deve possuir identificador.");
        }
        if (veiculo.getId() == null) {
            throw new RegraDeNegocioException("Veiculo da ordem de servico deve possuir identificador.");
        }
        this.funcionarioId = funcionario.getId();
        this.funcionarioNome = funcionario.getNome();
        this.funcionarioCpf = funcionario.getCpf();
        this.clienteId = cliente.getId();
        this.clienteNome = cliente.getNome();
        this.clienteDocumento = cliente.getCpfOuCnpj();
        this.clienteTipo = cliente.getTipoCliente();
        this.veiculoId = veiculo.getId();
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
