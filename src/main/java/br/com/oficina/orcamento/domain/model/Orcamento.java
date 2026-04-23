package br.com.oficina.orcamento.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "orcamentos")
public class Orcamento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String numeroOrcamento;

    @Column(nullable = false)
    private UUID clienteId;

    @Column(nullable = false)
    private UUID ordemDeServicoId;

    @Column(nullable = false)
    private UUID funcionarioId;

    @Column(nullable = false)
    private String clienteNome;

    @Column(nullable = false)
    private String clienteCpf;

    @Column(nullable = false)
    private String placaVeiculo;

    @Column(nullable = false)
    private String marcaVeiculo;

    @Column(nullable = false)
    private String modeloVeiculo;

    @Column(nullable = false, length = 4000)
    private String descricaoDiagnostico;

    @ElementCollection
    @CollectionTable(name = "orcamento_servicos_propostos", joinColumns = @JoinColumn(name = "orcamento_id"))
    @Column(name = "servico", nullable = false)
    private List<String> servicosPropostos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "orcamento_pecas_previstas", joinColumns = @JoinColumn(name = "orcamento_id"))
    private List<PecaOrcamento> pecasPrevistas = new ArrayList<>();

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorMaoDeObra;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorPecas;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @Column(nullable = false)
    private LocalDateTime validade;

    @Column(length = 4000)
    private String observacoes;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal desconto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusOrcamento status;

    private LocalDateTime enviadoParaAprovacaoEm;

    protected Orcamento() {
    }

    public Orcamento(
            String numeroOrcamento,
            UUID ordemDeServicoId,
            UUID funcionarioId,
            String clienteNome,
            String clienteCpf,
            String placaVeiculo,
            String marcaVeiculo,
            String modeloVeiculo,
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<PecaOrcamento> pecasPrevistas,
            BigDecimal valorMaoDeObra,
            BigDecimal desconto,
            LocalDateTime criadoEm,
            LocalDateTime validade,
            String observacoes,
            StatusOrcamento status) {
        this(
                numeroOrcamento,
                null,
                ordemDeServicoId,
                funcionarioId,
                clienteNome,
                clienteCpf,
                placaVeiculo,
                marcaVeiculo,
                modeloVeiculo,
                descricaoDiagnostico,
                servicosPropostos,
                pecasPrevistas,
                valorMaoDeObra,
                desconto,
                criadoEm,
                validade,
                observacoes,
                status);
    }

    public Orcamento(
            String numeroOrcamento,
            UUID clienteId,
            UUID ordemDeServicoId,
            UUID funcionarioId,
            String clienteNome,
            String clienteCpf,
            String placaVeiculo,
            String marcaVeiculo,
            String modeloVeiculo,
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<PecaOrcamento> pecasPrevistas,
            BigDecimal valorMaoDeObra,
            BigDecimal desconto,
            LocalDateTime criadoEm,
            LocalDateTime validade,
            String observacoes,
            StatusOrcamento status) {
        definirDados(
                numeroOrcamento,
                clienteId,
                ordemDeServicoId,
                funcionarioId,
                clienteNome,
                clienteCpf,
                placaVeiculo,
                marcaVeiculo,
                modeloVeiculo,
                descricaoDiagnostico,
                servicosPropostos,
                pecasPrevistas,
                valorMaoDeObra,
                desconto,
                criadoEm,
                validade,
                observacoes,
                status);
    }

    public static Orcamento reconstituir(
            UUID id,
            String numeroOrcamento,
            UUID ordemDeServicoId,
            UUID funcionarioId,
            String clienteNome,
            String clienteCpf,
            String placaVeiculo,
            String marcaVeiculo,
            String modeloVeiculo,
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<PecaOrcamento> pecasPrevistas,
            BigDecimal valorMaoDeObra,
            BigDecimal desconto,
            LocalDateTime criadoEm,
            LocalDateTime validade,
            String observacoes,
            StatusOrcamento status) {
        return reconstituir(
                id,
                numeroOrcamento,
                null,
                ordemDeServicoId,
                funcionarioId,
                clienteNome,
                clienteCpf,
                placaVeiculo,
                marcaVeiculo,
                modeloVeiculo,
                descricaoDiagnostico,
                servicosPropostos,
                pecasPrevistas,
                valorMaoDeObra,
                desconto,
                criadoEm,
                validade,
                observacoes,
                status);
    }

    public static Orcamento reconstituir(
            UUID id,
            String numeroOrcamento,
            UUID clienteId,
            UUID ordemDeServicoId,
            UUID funcionarioId,
            String clienteNome,
            String clienteCpf,
            String placaVeiculo,
            String marcaVeiculo,
            String modeloVeiculo,
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<PecaOrcamento> pecasPrevistas,
            BigDecimal valorMaoDeObra,
            BigDecimal desconto,
            LocalDateTime criadoEm,
            LocalDateTime validade,
            String observacoes,
            StatusOrcamento status) {
        Orcamento orcamento = new Orcamento(
                numeroOrcamento,
                clienteId,
                ordemDeServicoId,
                funcionarioId,
                clienteNome,
                clienteCpf,
                placaVeiculo,
                marcaVeiculo,
                modeloVeiculo,
                descricaoDiagnostico,
                servicosPropostos,
                pecasPrevistas,
                valorMaoDeObra,
                desconto,
                criadoEm,
                validade,
                observacoes,
                status);
        orcamento.id = id;
        return orcamento;
    }

    public void enviarParaAprovacao(LocalDateTime enviadoParaAprovacaoEm) {
        this.enviadoParaAprovacaoEm = enviadoParaAprovacaoEm;
        this.status = StatusOrcamento.AGUARDANDO_APROVACAO;
    }

    public void aprovar() {
        this.status = StatusOrcamento.APROVADO;
    }

    public void rejeitar() {
        this.status = StatusOrcamento.REJEITADO;
    }

    public UUID getId() {
        return id;
    }

    public String getNumeroOrcamento() {
        return numeroOrcamento;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getOrdemDeServicoId() {
        return ordemDeServicoId;
    }

    public UUID getFuncionarioId() {
        return funcionarioId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteCpf() {
        return clienteCpf;
    }

    public String getPlacaVeiculo() {
        return placaVeiculo;
    }

    public String getMarcaVeiculo() {
        return marcaVeiculo;
    }

    public String getModeloVeiculo() {
        return modeloVeiculo;
    }

    public String getDescricaoDiagnostico() {
        return descricaoDiagnostico;
    }

    public List<String> getServicosPropostos() {
        return List.copyOf(servicosPropostos);
    }

    public List<String> getPecasPrevistas() {
        return pecasPrevistas.stream()
                .map(PecaOrcamento::getDescricao)
                .toList();
    }

    public List<PecaOrcamento> getPecasOrcamento() {
        return List.copyOf(pecasPrevistas);
    }

    public BigDecimal getValorMaoDeObra() {
        return valorMaoDeObra;
    }

    public BigDecimal getValorPecas() {
        return valorPecas;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getValidade() {
        return validade;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public StatusOrcamento getStatus() {
        return status;
    }

    public LocalDateTime getEnviadoParaAprovacaoEm() {
        return enviadoParaAprovacaoEm;
    }

    private void definirDados(
            String numeroOrcamento,
            UUID clienteId,
            UUID ordemDeServicoId,
            UUID funcionarioId,
            String clienteNome,
            String clienteCpf,
            String placaVeiculo,
            String marcaVeiculo,
            String modeloVeiculo,
            String descricaoDiagnostico,
            List<String> servicosPropostos,
            List<PecaOrcamento> pecasPrevistas,
            BigDecimal valorMaoDeObra,
            BigDecimal desconto,
            LocalDateTime criadoEm,
            LocalDateTime validade,
            String observacoes,
            StatusOrcamento status) {
        this.numeroOrcamento = numeroOrcamento;
        this.clienteId = clienteId;
        this.ordemDeServicoId = ordemDeServicoId;
        this.funcionarioId = funcionarioId;
        this.clienteNome = clienteNome;
        this.clienteCpf = clienteCpf;
        this.placaVeiculo = placaVeiculo;
        this.marcaVeiculo = marcaVeiculo;
        this.modeloVeiculo = modeloVeiculo;
        this.descricaoDiagnostico = descricaoDiagnostico;
        this.servicosPropostos = new ArrayList<>(servicosPropostos);
        this.pecasPrevistas = new ArrayList<>(pecasPrevistas);
        this.valorMaoDeObra = valorMaoDeObra;
        this.valorPecas = pecasPrevistas.stream()
                .map(p -> p.getPreco().multiply(BigDecimal.valueOf(p.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.desconto = desconto == null ? BigDecimal.ZERO : desconto;
        this.valorTotal = valorMaoDeObra.add(this.valorPecas).subtract(this.desconto);
        this.criadoEm = criadoEm;
        this.validade = validade;
        this.observacoes = observacoes;
        this.status = status;
    }
}
